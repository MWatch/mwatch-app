package com.mabezdev.MabezWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mabez on 07/03/16.
 */
public class UUID {

    private static List<Integer> ids = new ArrayList<Integer>();
    private static final int RANGE = 10000;

    private static int index = 0;

    static {
        for (int i = 0; i < RANGE; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids);
    }

    private UUID() {

    }

    public static int getIdentifier() {
        if (index > ids.size() - 1) index = 0;
        return ids.get(index++);
    }
}
