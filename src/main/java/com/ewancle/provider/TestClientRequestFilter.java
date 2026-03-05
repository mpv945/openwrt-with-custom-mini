package com.ewancle.provider;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;

// 自定义请求
@Provider
public class TestClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().add("my_header", "value");
        String contentType = requestContext.getHeaderString("Content-Type");
        if ("application/json".equals(contentType)) {
            requestContext.getHeaders().putSingle("Content-Type", "application/yaml");
        }
        Providers providers = ((ResteasyReactiveClientRequestContext) requestContext).getProviders();

        // 🟢 场景 3：动态选择 MessageBodyWriter
        //根据请求类型自动选择不同序列化方式
        //例如 XML / JSON 混合请求
        /*MessageBodyWriter writer = providers.getMessageBodyWriter(MyData.class, MyData.class,
                new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE);*/

        // 🟢 场景 2：获取 ObjectMapper / 自定义序列化
        //可以复用 Quarkus 注册的 Jackson 配置
        //避免 new ObjectMapper() 导致配置不一致
        /*ContextResolver<ObjectMapper> resolver = providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE);
        ObjectMapper mapper = resolver.getContext(MyData.class);
        String json = mapper.writeValueAsString(new MyData("abc"));*/

        // 🟢 场景 1：自定义请求体序列化
        //✅ 优点：使用全局 Provider 逻辑，不需要手动创建 ObjectMapper
        /*MyData data = new MyData("abc");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

        MessageBodyWriter writer = providers.getMessageBodyWriter(
                MyData.class, MyData.class, new Annotation[]{}, mediaType
        );

        writer.writeTo(data, MyData.class, MyData.class, new Annotation[]{}, mediaType,
                requestContext.getHeaders(), baos);

        requestContext.setEntity(baos.toByteArray());
        requestContext.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);*/
    }
}
