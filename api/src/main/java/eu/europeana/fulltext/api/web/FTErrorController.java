package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.config.FTDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by luthien on 2019-08-13.
 */
@RestController
@ApiIgnore
public class FTErrorController extends AbstractErrorController {

    public FTErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }


    @RequestMapping(value = "/error", produces = {FTDefinitions.MEDIA_TYPE_JSON, FTDefinitions.MEDIA_TYPE_JSONLD})
    @ResponseBody
    public Map<String, Object> error(final HttpServletRequest request) {
        return this.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    }
}
