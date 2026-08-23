package io.github.wooongchan.requestflow.web;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 뷰어 리소스를 호스트 앱의 /static 네임스페이스와 충돌하지 않도록
 * 전용 classpath 경로(trace-viewer-static)에서 서빙한다.
 */
public class ViewerWebConfig implements WebMvcConfigurer {

    private final String viewerPath;

    public ViewerWebConfig(String viewerPath) {
        this.viewerPath = viewerPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(viewerPath + "/**")
                .addResourceLocations("classpath:/trace-viewer-static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(viewerPath, viewerPath + "/index.html");
    }
}
