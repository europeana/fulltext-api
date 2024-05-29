package eu.europeana.fulltext.loader.test;


import eu.europeana.fulltext.loader.exception.ArchiveNotFoundException;
import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.service.LoadArchiveService;
import eu.europeana.fulltext.loader.service.MongoService;
import eu.europeana.fulltext.loader.web.LoaderController;
import org.apache.logging.log4j.core.util.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the application's controller
 *
 * @author Patrick Ehlert Created on 12-01-2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:loader-test.properties")
@WebMvcTest(
    value = LoaderController.class,
    // disable security for this test
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class
    })
@Deprecated
public class LoaderControllerTest {

    private static final String ZIP_PROCESSED_OK = "Finished ok";
    private static final long DELETED_ITEMS = 10;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoadArchiveService loadArchiveService;
    @MockBean
    private MongoService mongoService;

    @Before
    public void setup() throws LoaderException {
        given(loadArchiveService.importZipBatch(any(), any())).willReturn(ZIP_PROCESSED_OK);
        given(loadArchiveService.importZipBatch( eq("notExists.zip"), any())).willThrow(new ArchiveNotFoundException("Not found"));
        given(loadArchiveService.importZipBatch( eq("readError.zip"), any())).willThrow(new ArchiveReadException("Read error"));

        given(mongoService.deleteAllAnnoPages(eq("9200357"))).willReturn(DELETED_ITEMS);
        given(mongoService.deleteAllResources(eq("9200357"))).willReturn(DELETED_ITEMS);
    }

    /**
     * Test processing zip-batch request
     */
    @Test
    public void testZipBatchOk() throws Exception {
        this.mockMvc.perform(get("/fulltext/zipbatch?archive=9200357.zip"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(ZIP_PROCESSED_OK));
    }

    /**
     * Test processing zip-batch request where zip cannot be found
     */
    @Test
    public void testZipBatchErrorNotFound() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/fulltext/zipbatch?archive=notExists.zip"))
                .andExpect(status().is(404)).andReturn();
        // make sure there is some feedback on the error to the user
        Assert.isNonEmpty(result.getResponse().getContentAsString());
    }

    /**
     * Test processing zip-batch request where zip cannot be read properly
     */
    @Test
    public void testZipBatchErrorRead() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/fulltext/zipbatch?archive=readError.zip"))
                .andExpect(status().is(500)).andReturn();
        // make sure there is some feedback on the error to the user
        Assert.isNonEmpty(result.getResponse().getContentAsString());
    }

    /**
     * Test processing delete request
     */
    @Test
    public void testDelete() throws Exception {
        this.mockMvc.perform(get("/fulltext/delete?datasetId=9200357"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted " + DELETED_ITEMS + " annopages and "
                        + DELETED_ITEMS + " resources"));
    }


}
