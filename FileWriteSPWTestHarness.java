  /*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2014 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import SPW.FileWriteSPW;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import loci.formats.FormatTools;
 

/**
 * Example class that shows how to export raw pixel data to OME-TIFF as a Plate using
 * Bio-Formats version 4.2 or later.
 */
public class FileWriteSPWTestHarness {
  
  /**
   * Constructor
   *
   */
  public FileWriteSPWTestHarness() {         
  }
  
  /**
   * Generate a plane of pixel data.
   *
   * @param width the width of the image in pixels
   * @param height the height of the image in pixels
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
 
  private byte[] createImage(int width, int height, int pixelType, int index, int series) {
    // create a blank image of the specified size
    int bpp = FormatTools.getBytesPerPixel(pixelType);
    byte[] img = new byte[width * height * bpp];
    
    ByteBuffer bb = ByteBuffer.wrap(img);
    bb.order(ByteOrder.BIG_ENDIAN);
    
    // fill it with background 
    for (int i = 0; i < img.length; i += bpp) {
      bb.putShort(i, (short) 200);
    }

    //then set 1 pixel to non-zero. Different values in each plane
    switch (index) {
      case 0: bb.putShort(series * bpp, (short) 1000);
              break;
      case 1: bb.putShort(series * bpp, (short) 700);
              break;
      case 2: bb.putShort(series * bpp, (short) 300);
              break;
    }  
   
    return img;
  }
  
  

  /**
   * To export a file to OME-TIFF:
   *
   */
  public static void main(String[] args) throws Exception {
    
    String fileOut = "SPWFromJava.ome.tiff";
   
    int nFOV = 2;
    int nRows = 2;
    int nCols = 2;
    
     // set up ome-tiff writer here
    int pixelType = FormatTools.UINT16;
    
    int[][] nFovInWell = new int[nRows][nCols];
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nCols; col++) {
        nFovInWell[row][col] = nFOV;
      }
    }
    int sizeX = 4;
    int sizeY = 4;
    int sizet = 3;
    
    String plateDescription = "FLIM test plate data";
    FileWriteSPW SPWWriter = new FileWriteSPW(fileOut, plateDescription);
    FileWriteSPWTestHarness reader = new FileWriteSPWTestHarness();
    
    byte[] plane;
    ArrayList<String> delayList = new ArrayList<>();
    delayList.add("1000");
    delayList.add("2000");
    delayList.add("3000");
    
    double[] exposureTimes = new double[sizet];
    for (int t = 0; t < sizet; t++)  {
      exposureTimes[t] = 1000.0;
    }
    
    boolean ok = SPWWriter.init(nFovInWell, sizeX, sizeY, sizet, delayList, exposureTimes);
    
    //alternative setup for non-FLIM data
    //sizet = 1;
    //boolean ok = SPWWriter.init(nFovInWell, sizeX, sizeY);
    
    int nImages = nRows * nCols * nFOV;
    
    // simulate an Abort  by not writing alll the FOVS !!
    nImages -= 2;
    
    if (ok)  {
      for (int f = 0; f< nImages; f++)  { 
        for (int t = 0; t< sizet; t++)  {
          plane = reader.createImage(sizeX, sizeY, pixelType, t, f);
          String imageDescription = "Description" + Integer.toString(f);
          SPWWriter.export(plane, f, t, imageDescription);
        }
      }
      SPWWriter.cleanup();
    }  
  }  
}

