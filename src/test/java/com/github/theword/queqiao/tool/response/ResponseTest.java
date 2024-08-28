package com.github.theword.queqiao.tool.response;

import org.junit.jupiter.api.Test;


class ResponseTest {

    @Test
    void getJson() {
        Response response = new Response(200, ResponseEnum.SUCCESS, "success", null, null);
        System.out.println(response.getJson());
    }

}