package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.client.AdminSkillClient;
import com.cloud.programmer.runtime.skill.Skill;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class RestAdminSkillClient implements AdminSkillClient {

    private final RestClient restClient;

    public RestAdminSkillClient(
            @Qualifier("adminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Skill> findSkillByAgentId(Long agentId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/skill/findSkillByAgentId")
                        .queryParam("agentId", agentId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Skill>>() {});
    }
}