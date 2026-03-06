package com.ewancle.service;

import com.ewancle.dto.Extension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Set;

@RegisterRestClient(configKey = "remote-api") // 找到配置文件的配置信息
public interface OpenwrtRestClientService {

    @GET
    @Path("/extensions")
    Set<Extension> getExtensionsById(@QueryParam("id") String id);
}
