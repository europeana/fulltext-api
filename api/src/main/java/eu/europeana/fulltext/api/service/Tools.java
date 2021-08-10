package eu.europeana.fulltext.api.service;

import org.springframework.util.StringUtils;

/**
 * Created by luthien on 07/08/2021.
 */
public class Tools {


    public static String nvl(String input){
        return StringUtils.isEmpty(input) ? "null" : input;
    }

}
