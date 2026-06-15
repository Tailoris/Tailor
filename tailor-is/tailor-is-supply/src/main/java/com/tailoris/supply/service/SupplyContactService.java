package com.tailoris.supply.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.supply.dto.ContactRequest;
import com.tailoris.supply.entity.SupplyContactRecord;

public interface SupplyContactService {

    SupplyContactRecord createContact(Long userId, ContactRequest request);

    PageResponse<SupplyContactRecord> listContacts(Long userId, PageRequest pageRequest);

    void respondContact(Long contactId, Long userId, String replyMessage);
}
