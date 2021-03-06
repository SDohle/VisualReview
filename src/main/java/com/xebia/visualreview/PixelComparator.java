/*
 * Copyright 2015 Xebia B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xebia.visualreview;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

import clojure.lang.Keyword;


public class PixelComparator {

    private static int DIFFCOLOUR = (128<<24) & 0xff000000|       //a
            (128<<16) &   0xff0000|       //r
            (0  <<8 ) &     0xff00|       //g
            (0  <<0 ) &       0xff;       //b

    private static class Rect{
        private int x;
        private int y;
        private int width;
        private int height;
        public boolean isValid;
        Rect(int x,int y,int width, int height){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            isValid = true;
        }
        Rect(java.util.Map info, int maxWidth, int maxHeight){
            isValid = true;
            try{
                int x = Integer.parseInt(info.get(Keyword.find("x")).toString());
                int y = Integer.parseInt(info.get(Keyword.find("y")).toString());
                int width = Integer.parseInt(info.get(Keyword.find("width")).toString());
                int height = Integer.parseInt(info.get(Keyword.find("height")).toString());

                this.x = Math.min(Math.max(0,x),maxWidth);
                this.y = Math.max(Math.min(this.y,maxHeight),y);
                this.width = Math.min(Math.max(0,width),maxWidth-this.x);
                this.height = Math.min(Math.max(0,height),maxHeight-this.y);
            }catch (Exception e) {
                System.out.println("Invalid rect "+info);
                isValid = false;
                throw new RuntimeException(new Exception("Invalid Rect "+info));
            }
        }

        void applyToImage(BufferedImage image,int rgb){
            if (isValid){
                for(int i = x;i<x+width;i++){
                    for(int j = y;j<y+height;j++){
                        image.setRGB(i,j,rgb);
                    }
                }
            }
        }
    }

    private static BufferedImage generateMask(java.util.Map maskInfo, int width, int height){
        BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Rect fullRect = new Rect(0,0,width,height);
        if (maskInfo != null) {
            java.util.List<java.util.Map> excludeszones = (java.util.List<java.util.Map>)maskInfo.get(Keyword.find("excludeZones"));
            if (excludeszones!=null) {
                for (int i = 0; i < excludeszones.size(); i++) {
                    java.util.Map excludeZone = excludeszones.get(i);
                    Rect rect = new Rect(excludeZone, width, height);
                    rect.applyToImage(maskImage, DIFFCOLOUR);
                }
            }
        }
        return maskImage;
    }

    public static DiffReport processImage(File beforeFile, File afterFile ,java.util.Map maskInfo) {
        try {
            PixelGrabber beforeGrab = grabImage(beforeFile);
            PixelGrabber afterGrab = grabImage(afterFile);

            int[] beforeData = null;
            int[] afterData = null;
            int beforeWidth = 0;
            int afterWidth = 0;
            int y1 = 0;
            int y2 = 0;

            if (beforeGrab.grabPixels()) {
                beforeWidth = beforeGrab.getWidth();
                y1 = beforeGrab.getHeight();
                beforeData = (int[]) beforeGrab.getPixels();
            }

            if (afterGrab.grabPixels()) {
                afterWidth = afterGrab.getWidth();
                y2 = afterGrab.getHeight();
                afterData = (int[]) afterGrab.getPixels();
            }

            int minX = Math.min(beforeWidth, afterWidth);
            int minY = Math.min(y1, y2);
            int diffWidth = Math.max(beforeWidth, afterWidth);
            int diffHeight = Math.max(y1, y2);
            int[] diffData = new int[diffWidth * diffHeight];
            int differentPixels = 0;
            boolean hasMask =  (maskInfo != null) ;
            BufferedImage maskImage = generateMask(maskInfo,diffWidth,diffHeight);
            for (int y = 0; y < diffHeight; y++) {
                for (int x = 0; x < diffWidth; x++) {
                    if (maskImage.getRGB(x,y) != DIFFCOLOUR) {
                        if (x >= minX || y >= minY || beforeData[y * beforeWidth + x] != afterData[y * afterWidth + x]) {
                            diffData[y * diffWidth + x] = DIFFCOLOUR;
                            differentPixels++;
                        }
                    }
                }
            }

            BufferedImage diffImage = new BufferedImage(diffWidth, diffHeight, BufferedImage.TYPE_INT_ARGB);
            diffImage.setRGB(0, 0, diffWidth, diffHeight, diffData, 0, diffWidth);

            DiffReport report = new DiffReport(beforeFile, afterFile, diffImage, differentPixels);
            if (hasMask){
                report.setMaskImage(maskImage);
            }
            return report;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static PixelGrabber grabImage(File file) {
        try {
            return new PixelGrabber(loadImage(file), 0, 0, -1, -1, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
