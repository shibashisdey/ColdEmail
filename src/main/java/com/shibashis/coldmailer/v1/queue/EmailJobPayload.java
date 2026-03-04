package com.shibashis.coldmailer.v1.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailJobPayload {
    private Long campaignId;
    private Long campaignContactId;
    private Long userId;
    private UUID trackingId;
}
