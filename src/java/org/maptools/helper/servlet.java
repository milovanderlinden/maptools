/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.maptools.helper;

import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author milo
 */
public class servlet implements java.io.Serializable {

    public static String getCaseInsensitiveParam(Map params, String param) {
        Iterator it = params.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (key.equalsIgnoreCase(param)) {
                String[] my = (String[]) params.get(key);
                return (String) my[0];
            }
        }
        return null;
    }
}
