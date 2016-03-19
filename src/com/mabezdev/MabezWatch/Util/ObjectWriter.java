package com.mabezdev.MabezWatch.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

/**
 * Created by Mabez on 19/03/2016.
 */
public class ObjectWriter {

    private static ObjectOutputStream writer;

    private ObjectWriter(){

    }

    public static boolean writeObject(String filePath, Object toWrite){
        try {
            writer = new ObjectOutputStream(new FileOutputStream(filePath));
            writer.writeObject(toWrite);
            writer.close();
        } catch (NotSerializableException e){
            e.printStackTrace();
            return false;
        } catch (IOException e2){
            e2.printStackTrace();
            return false;
        }
        return true;
    }
}
