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
import java.nio.file.Path;
import java.io.File;
import java.util.ArrayList;

import loci.formats.services.OMEXMLService;
import loci.formats.ChannelFiller;

import loci.formats.ChannelSeparator;
import loci.formats.FormatTools;
import loci.formats.services.OMEXMLServiceImpl;
 

/**
 * Example class that shows how to export raw pixel data to OME-TIFF as a Plate using
 * Bio-Formats version 4.2 or later.
 */
public class TiffStackToSPW {
  
  /**
   * Constructor
   *
   */
  public TiffStackToSPW() {         
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
    
    // Directory path here
    String path = "/Users/imunro/globalprocessing/GlobalProcessingFrontEnd/ParisMeetingData/2012-07-26_16-59-47";
    String fileOut  = path + "/" + "SPWFromJava.ome.tiff";
    
    
    String subdir;
    String[] subdirs;
    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();

    int nFOV = 0;
    String wellStr;
    String temp[];
    int row;
    int col;

    int nRows = 0;
    int nCols = 0;
    
    ArrayList<String> dirList = new ArrayList<String>();

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isDirectory()) {
        subdir = listOfFiles[i].getName();
        if (subdir.contains("FOV")) {
          dirList.add(subdir);
          temp = subdir.split(" ");
          wellStr = temp[0];
          temp = wellStr.split("-");
          row = temp[0].charAt(0) - 'A';
          if (temp[1].length() == 1)  {
            col = temp[1].charAt(0) - '1';
          } else {
            col = 10 * (temp[1].charAt(0) - '0');
            col += temp[1].charAt(1) - '0';
            col--;
          }
          if (row + 1> nRows) {
            nRows = row + 1;
          }
          if (col + 1> nCols) {
            nCols = col + 1;
          }
          
        }
      }
    }
    
    
    
    int[][] nFovInWell = new int[nRows][nCols];
    for (row = 0; row < nRows; row++) {
      for (col = 0; col < nCols; col++) {
        nFovInWell[row][col] = 0;
      }
    }

    for (int f = 0; f < dirList.size(); f++) {
      subdir = dirList.get(f);
      temp = subdir.split(" ");
      wellStr = temp[0];
      temp = wellStr.split("-");
      row = temp[0].charAt(0) - 'A';
  
      if (temp[1].length() == 1)  {
        col = temp[1].charAt(0) - '1';
      }
      else  {
        col = 10 * (temp[1].charAt(0) - '0');
        col+= temp[1].charAt(1) - '0';
        col--;   
      }
      nFovInWell[row][col]++;
      nFOV++;
    }
    
    // use 1st subdir to get sizet and delays
    subdir = dirList.get(0);
    String subPath = path + "/" + subdir;
    File subFolder = new File(subPath);
    listOfFiles = subFolder.listFiles();
    String fname = " ";
    String delayStr;
    ArrayList<String> delayList = new ArrayList<String>();
    ArrayList<String> fnameList = new ArrayList<String>();
    int sizet = 0;
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        fname = listOfFiles[i].getName();
        if (fname.endsWith(".tif")) {
          sizet++;
          fnameList.add(fname);
          temp = fname.split(" ");
          // NB temp[0] contains integration time
          delayStr = temp[1];
          String tempStr = delayStr.substring(0, delayStr.length() - 4); // drop extension
          delayStr = tempStr.substring(2);   // drop the "T_" at the beginning
          // leading zeros
          while (delayStr.startsWith("0"))
            delayStr = delayStr.substring(1);
          delayList.add(delayStr);
        }
      }
    }
    
    
    
    String id = subPath + "/" + fnameList.get(0);
    
    // set up a reader for a file in the subdir
    ChannelFiller r = new ChannelFiller();
    ChannelSeparator reader = new ChannelSeparator(r);
    OMEXMLServiceImpl service = new OMEXMLServiceImpl();
    reader.setMetadataStore(service.createOMEXMLMetadata());
    
    reader.setId(id);
    int sizeX = reader.getSizeX();
    int sizeY = reader.getSizeY();
   
    // set up ome-tiff writer here
    FileWriteSPW SPWWriter = new FileWriteSPW(fileOut);
    Double[] exposureTimes = new Double[sizet];
    for (int t = 0; t < sizet; t++)  {
      exposureTimes[t] = 1000.0;
    }
    
    boolean ok = SPWWriter.init(nFovInWell, sizeX, sizeY, sizet, delayList, exposureTimes);
    byte[] plane;
    
    if (ok)  {
      for (int f = 0; f< nFOV; f++)  {
        subdir = dirList.get(f);
        subPath = path + "/" + subdir;
        for (int t = 0; t< sizet; t++)  {
          fname = fnameList.get(t);
          id = subPath + "/" + fname;
          reader.setId(id);
          plane = reader.openBytes(0);
          
          // code to handle the ridiculous Labview signed storage!
          ByteBuffer bb = ByteBuffer.wrap(plane); // Wrapper around underlying byte[].
          bb.order(ByteOrder.LITTLE_ENDIAN);
          short s;
          short min = (short)32768;
          for (int i = 0; i < plane.length ; i+=2) {
            s = (short)bb.getShort(i);
            if (s < min ) { 
              min = s;
              break;       
            } 
          }
          
          if (min > 32767)  {
            for (int i = 0; i < plane.length ; i+=2) {
              s = (short)bb.getShort(i);
              bb.putShort(i, (short) (s - 32768));
            }
          }        
          
          SPWWriter.export(plane, f, t);
        }
      }
      
      SPWWriter.cleanup();
      } 
     
  }  
}

