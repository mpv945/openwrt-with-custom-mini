package com.ewancle.dto;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

public class DownloadForm {
    @RestForm
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("otherField")
    @PartType(MediaType.TEXT_PLAIN)
    public String textProperty;
}
