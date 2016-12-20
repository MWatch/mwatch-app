package com.mabezdev.MabezWatch.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Mabez on 19/03/2016.
 */
public class ObjectReader{

    private static ObjectInputStream reader;

    private ObjectReader(){

    }

    public static Object readObject(String path){
        Object read = null;
        try {
            reader = new ObjectInputStream(new FileInputStream(path));
            read = reader.readObject();
            reader.close();
        }catch (IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e2){
            e2.printStackTrace();
        }
        return read;
    }
}
