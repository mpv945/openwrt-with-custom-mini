package com.ewancle.service;

import com.ewancle.dto.Extension;
import com.ewancle.dto.User;
import com.ewancle.dto.Views;
import com.ewancle.provider.TestClientRequestFilter;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.rest.client.reactive.NotBody;
import io.quarkus.rest.client.reactive.Url;
import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.*;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/*@Path("/extensions")
//@RegisterRestClient
@RegisterRestClient(configKey = "remote-api") // 找到配置文件的配置信息
//@RegisterClientHeaders
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
//固定动态添加参数，${my.property-value}表示从配置文件读取，对接口下所有请求有效
//@ClientQueryParam(name = "my-param", value = "${my.property-value}")
@ClientQueryParam(name = "ts", value = "{now}")
@RegisterProvider(TestClientRequestFilter.class)
//@ClientBasicAuth(username = "${service.username}", password = "${service.password}")*/
public interface RestClientService {

    /*@GET
    @ClientHeaderParam(name = "header-from-properties", value = "${header.value}")
    @ClientHeaderParam(name = "header-from-method-param", value = "Bearer {token}")
    Set<Extension> getById(@QueryParam("id") String id, @HeaderParam("jaxrs-style-header") String headerValue, @NotBody String token);

    // REST 客户端支持添加自定义 ObjectMapper
    @ClientObjectMapper
    static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
        return defaultObjectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }

    @ClientExceptionMapper
    @Blocking
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 500) {
            response.readEntity(String.class);
            return new RuntimeException("The remote service responded with HTTP 500");
        }
        return null;
    }

    //
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(Views.Public.class)
    User get(@RestPath String id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(@JsonView(Views.Public.class) User user);

    // 上传文件
    @POST
    @Path("/binary")
    //@ClientHeaderParam(name = "Content-Type", value = "{calculateContentType}")  头信息
    String sendMultipart(@RestForm File file, @RestForm String otherField);

    // 大文件用 Path 而不是 File
    // 调用
    // public Uni<String> upload(Path path) {
    //    return fileClient.upload(path, path.getFileName().toString());
    //}
    Uni<String> upload(
            @RestForm("file")
            @PartType(MediaType.APPLICATION_OCTET_STREAM)
            Path file,

            @RestForm("filename")
            String fileName
    );
    // 下载
    // public Uni<Void> download(String name, Path target) {
    //
    //    return fileClient.download(name)
    //            .onItem().transformToUni(resp ->
    //                Uni.createFrom().item(() -> {
    //                    try (InputStream in = resp.readEntity(InputStream.class)) {
    //                        Files.copy(in, target);
    //                        return null;
    //                    }
    //                }).runSubscriptionOn(
    //                    io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor()
    //                )
    //            );
    //}
    @GET
    @Path("/{name}")
    Uni<jakarta.ws.rs.core.Response> download(
            @PathParam("name") String name);

    @POST
    @Path("/json")
    String sendMultipart(@RestForm @PartType(MediaType.APPLICATION_JSON) Person person);
    public static class Person {
        public String firstName;
        public String lastName;
    }

    // 固定添加参数，可动态，也可全局
    static String now() {
        return String.valueOf(System.currentTimeMillis());
    }

    @GET
    @ClientQueryParam(name = "some-other-param", value = "other")
    String getWithOtherParam();

    @GET
    @ClientQueryParam(name = "param-from-method", value = "{with-param}") // 动态参数
    String getFromMethod();
    default String withParam(String name) {
        if ("param-from-method".equals(name)) {
            return "test";
        }
        throw new IllegalArgumentException();
    }

    // 动态基本URL：当该url参数不为空时，它将覆盖为客户端配置的基本 URL（默认基本 URL 配置仍然是必需的）
    @GET
    @Path("/stream/{stream}")
    Set<Extension> getByStream(@Url String url, @PathParam("stream") String stream, @QueryParam("id") String id);

    // 路径参数
    @GET
    @Path("/stream/{stream}")
    Set<Extension> getByStream(@PathParam("stream") String stream, @QueryParam("id") String id);

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    Set<Extension> getByName(@RestQuery String name);

    @GET
    Set<Extension> getByOptionalName(@RestQuery Optional<String> name);

    @GET
    Set<Extension> getByFilter(@RestQuery Map<String, String> filter);

    @GET
    Set<Extension> getByFilters(@RestQuery MultivaluedMap<String, String> filters);

    @GET
    @Path("/data")
    Uni<String> getData();

    @GET
    RestResponse<Set<Extension>> getByIdResponseProperties(@RestQuery String id);

    // 表单参数
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postId(@FormParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postName(@RestForm String name);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postFilter(@RestForm Map<String, String> filter);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postFilters(@RestForm MultivaluedMap<String, String> filters);

    // 异步： 调用重试：extensionsService.getByIdAsUni(id).onFailure().retry().atMost(10);
    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam("id") String id);
    @GET
    Uni<Set<Extension>> getByIdAsUni(@QueryParam("id") String id);

    // SSE
    *//*@GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> get();*//*
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseEventFilter(HeartbeatFilter.class)
    Multi<SseEvent<Long>> get();
    class HeartbeatFilter implements Predicate<SseEvent<String>> {
        @Override
        public boolean test(SseEvent<String> event) {
            return !"heartbeat".equals(event.id());
        }
    }*/
}
