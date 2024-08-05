package com.github.theword.queqiao.payload;

import com.github.theword.queqiao.payload.modle.CommonSendTitle;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SendTitlePayload {

    @SerializedName("send_title")
    private CommonSendTitle commonSendTitle;

}
