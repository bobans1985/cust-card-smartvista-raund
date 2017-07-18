package com.custsystems.integration.processing.smartvista_raund.answer.status;

import com.custsystems.custtools.XmlTools;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.CardResult;
import com.custsystems.integration.card.custom.answer.CardStatus;
import com.custsystems.integration.card.custom.status.ProcessingProductStatus;
import com.custsystems.integration.exception.IntegrationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by GrishukovVM on 13.09.2016.
 */
public class SmartVistaRaundStatus extends CardStatus {

    public SmartVistaRaundStatus(Object data) throws ProcessingRequestException, ProcessingControlException {
        super(data);
    }

    @Override
    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        if (!(data instanceof Document)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate response object not instance of XML document!");
        } else {
            Document document = (Document) data;
            Element e1 = document.getDocumentElement();
            Element cardStatus = XmlTools.getBranchElement(e1, "Body/cardStatusInquiryResponse");
            String code = XmlTools.getStringEx(cardStatus, "hotCardStatus");
            this.setStatus(getProductStatus(code));
        }
    }

    // Нужно сделать нормальное соответствие
    protected ProcessingProductStatus getProductStatus(String status) {
        ProcessingProductStatus retStat = ProcessingProductStatus.UNKNOWN;
        switch (status) {
            case "0":
                retStat = ProcessingProductStatus.VALID;
            case "5":
                retStat = ProcessingProductStatus.BLOCKED;
        }
        return retStat;
    }

}
