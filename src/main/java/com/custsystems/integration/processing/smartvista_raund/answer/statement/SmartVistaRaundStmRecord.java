package com.custsystems.integration.processing.smartvista_raund.answer.statement;

import com.custsystems.custtools.DateTools;
import com.custsystems.custtools.XmlTools;
import com.custsystems.ibankfl.model.place.EmptyGeographicCoordinates;
import com.custsystems.ibankfl.model.place.GeographicCoordinates;
import com.custsystems.ibankfl.tools.currency.CurrencyCode;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.CardStatementRecord;
import com.custsystems.integration.card.pos.PointOfSale;
import com.custsystems.integration.processing.cardstandard.CardStandardUtils;
import org.w3c.dom.Element;

import java.math.BigDecimal;

public class SmartVistaRaundStmRecord extends CardStatementRecord {
    public SmartVistaRaundStmRecord(Object data) throws ProcessingControlException, ProcessingRequestException {
        super(data);
    }

    public static BigDecimal fixResponseAmount(Element e, String name, String feeDirection) {
        BigDecimal retValue = BigDecimal.valueOf(0);
        if (XmlTools.getStringEx(e, feeDirection).equals("debit"))
            retValue = BigDecimal.valueOf(XmlTools.getLongEx(e, name).longValue(), 2).negate();
        else retValue = BigDecimal.valueOf(XmlTools.getLongEx(e, name).longValue(), 2);
        return retValue;
    }

    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        if (!(data instanceof Element)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate statement record object not instance of XML element!");
        } else {
            Element element = (Element) data;

            //Параметры точки совершения
            PointOfSale pos = new PointOfSale();
            pos.setMcc(XmlTools.getStringEx(element, "mcc", (String) null));//
            //pos.setTerminalId(XmlTools.getStringEx(element, "merchantId", (String) null));//
            String spName = XmlTools.getStringEx(element, "terminalAddress", (String) null);
            pos.setName(spName);//
            pos.setCoordinates(new EmptyGeographicCoordinates());
            this.setPointOfSale(pos);



            /*Дата и время операции и транзакции*/
            this.setPostingDate(DateTools.getTimestamp(XmlTools.getStringEx(element, "authorizationDate"), "yyyy-MM-dd'T'HH:mm:ss"));//
            this.setTransDate(this.getPostingDate());

            this.setTransDetails(XmlTools.getStringEx(element, "transactionDescription"));//
            //this.setTrnType("test");

            this.setApprovalCode(XmlTools.getStringEx(element, "authorizationIdResponse", (String) null)); //utrnno
            this.setMerchantId(XmlTools.getStringEx(element, "merchantId", (String) null));//


            //Суммы операции и их валюты
            this.setTransAmount(fixResponseAmount(element, "amount", "feeDirection"));//
            this.setTransCurrency(new CurrencyCode(XmlTools.getStringEx(element, "currency")));//
            this.setAccountAmount(fixResponseAmount(element, "amountInAccountCurrency", "feeDirection"));//

            this.setAccountCurrency(this.getTransCurrency()); //фиг знает, где вытянуть


            this.setHold(Boolean.valueOf(false));
            this.setTransaction(Boolean.valueOf(true));


        }
    }
}
