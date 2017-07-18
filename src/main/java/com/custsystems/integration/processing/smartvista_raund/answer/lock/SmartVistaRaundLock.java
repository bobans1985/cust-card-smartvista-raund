package com.custsystems.integration.processing.smartvista_raund.answer.lock;

import com.custsystems.custtools.XmlTools;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.CardResult;
import com.custsystems.integration.exception.IntegrationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by GrishukovVM on 08.09.2016.
 */
public class SmartVistaRaundLock extends CardResult {

    public SmartVistaRaundLock(Object data) throws ProcessingControlException, ProcessingRequestException {
        super(data);
    }

    @Override
    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        if (!(data instanceof Document)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate response object not instance of XML document!");
        } else {

            Document document = (Document) data;
            Element e1 = document.getDocumentElement();
            Element cardLockResponse = XmlTools.getBranchElement(e1, "Body/blockCardResponse");
            String code = XmlTools.getStringEx(cardLockResponse, "blockCardResponse");
            //Будем считать, что если есть вообще XML ответ - то все ок
            this.setOk(true);
        }
    }
}

