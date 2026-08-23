package io.github.wooongchan.requestflow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "request-flow")
public class RequestFlowProperties {

    /**
     * 요청 흐름 계측 기능 전체 스위치. 운영 환경에서는 false로 끄거나
     * 의존성 자체를 dev/local 전용 구성으로 분리하는 것을 권장한다.
     */
    private boolean enabled = true;

    /**
     * 계측 대상 빈을 찾을 base package 목록 (예: com.mycompany, com.mycompany.order).
     * 비어 있으면 계측이 비활성화된다.
     */
    private List<String> basePackages = new ArrayList<>();

    /** 인메모리 링버퍼에 보관할 최근 요청 개수. */
    private int maxTraces = 50;

    /** 컬렉션/맵 인자·리턴값을 직렬화할 때 남길 최대 원소 개수. */
    private int maxCollectionSize = 50;

    /** 직렬화된 값 하나(JSON 문자열)의 최대 길이. */
    private int maxValueLength = 5000;

    /** 필드명이 이 패턴들에 부분일치(대소문자 무시)하면 값을 마스킹한다. */
    private List<String> maskFieldPatterns = new ArrayList<>(List.of(
            "password", "secret", "token", "authorization", "apikey", "ssn", "cardnumber", "pin"));

    /** 뷰어 UI/API가 노출되는 base path. */
    private String viewerPath = "/trace-viewer";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public int getMaxTraces() {
        return maxTraces;
    }

    public void setMaxTraces(int maxTraces) {
        this.maxTraces = maxTraces;
    }

    public int getMaxCollectionSize() {
        return maxCollectionSize;
    }

    public void setMaxCollectionSize(int maxCollectionSize) {
        this.maxCollectionSize = maxCollectionSize;
    }

    public int getMaxValueLength() {
        return maxValueLength;
    }

    public void setMaxValueLength(int maxValueLength) {
        this.maxValueLength = maxValueLength;
    }

    public List<String> getMaskFieldPatterns() {
        return maskFieldPatterns;
    }

    public void setMaskFieldPatterns(List<String> maskFieldPatterns) {
        this.maskFieldPatterns = maskFieldPatterns;
    }

    public String getViewerPath() {
        return viewerPath;
    }

    public void setViewerPath(String viewerPath) {
        this.viewerPath = viewerPath;
    }
}
