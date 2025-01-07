import enums

# all: free mutation on datatype (int, string, datetime, boolean)
# range: range mutation on datatype (enum, map)
# skip: invariant field (string, list, map)

field_list = {
    "#request_IgtpAdvanceFundRequest#  allocationRequestId": {
        "name": "allocationRequestId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  beneficiaryAgentId": {
        "name": "beneficiaryAgentId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  transferFromCurrency": {
        "name": "transferFromCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#request_IgtpAdvanceFundRequest#  transferToAmount": {
        "name": "transferToAmount",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  transferFromAmount": {
        "name": "transferFromAmount",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  payerAgentId": {
        "name": "payerAgentId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  transferToCurrency": {
        "name": "transferToCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#request_IgtpAdvanceFundRequest#  fxType": {
        "name": "fxType",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#request_IgtpAdvanceFundRequest#  quoteId": {
        "name": "quoteId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#    igtpResultCode": {
        "name": "igtpResultCode",
        "type": "range",
        "datatype": "enum",
        "extend": enums.IgtpCommonResultCode
    },
    "#FundInnerServiceMock#      baseCurrency": {
        "name": "baseCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#FundInnerServiceMock#      quoteCurrencyPair": {
        "name": "quoteCurrencyPair",
        "type": "range",
        "datatype": "mixed",
        "extend": {
            "len": 2,
            "mode": [
                enums.currency,
                enums.currency
            ],
            "sep": [
                "/"
            ]
        }
    },
    "#FundInnerServiceMock#      price": {
        "name": "price",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#FundInnerServiceMock#      participantPrice": {
        "name": "participantPrice",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#FundInnerServiceMock#      quoteReferenceId": {
        "name": "quoteReferenceId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#      participantId": {
        "name": "participantId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#      consumer": {
        "name": "consumer",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#      quoteType": {
        "name": "quoteType",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteTypeEnum
    },
    "#FundInnerServiceMock#        quoteConfigId": {
        "name": "quoteReferenceId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        startTime": {
        "name": "startTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#        expiryTime": {
        "name": "expiryTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#        participantId": {
        "name": "participantId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        quoteType": {
        "name": "quoteType",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteTypeEnum
    },
    "#FundInnerServiceMock#        sellCurrency": {
        "name": "sellCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#FundInnerServiceMock#        buyCurrency": {
        "name": "buyCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#FundInnerServiceMock#        consumePolicy": {
        "name": "consumePolicy",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteConsumePolicyEnum
    },
    "#FundInnerServiceMock#        queryPolicy": {
        "name": "queryPolicy",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteQueryPolicyEnum
    },
    "#FundInnerServiceMock#        productCode": {
        "name": "productCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        eventCode": {
        "name": "eventCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        lockPriceTime": {
        "name": "lockPriceTime",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#FundInnerServiceMock#        increaseType": {
        "name": "increaseType",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        increaseValue": {
        "name": "increaseValue",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#FundInnerServiceMock#        disableSourceValidate": {
        "name": "disableSourceValidate",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        status": {
        "name": "status",
        "type": "range",
        "datatype": "enum",
        "extend": enums.BaseStatusEnum
    },
    "#FundInnerServiceMock#        tntInstId": {
        "name": "tntInstId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        regionCode": {
        "name": "regionCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#        gmtCreate": {
        "name": "gmtCreate",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#        gmtModified": {
        "name": "gmtModified",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#      startTime": {
        "name": "startTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#      quoteUnit": {
        "name": "quoteUnit",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#FundInnerServiceMock#      status": {
        "name": "status",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteStatusEnum
    },
    "#FundInnerServiceMock#      bizProductCode": {
        "name": "bizProductCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#      bizEventCode": {
        "name": "bizEventCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#FundInnerServiceMock#      sourceGenerateTime": {
        "name": "sourceGenerateTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#      sourceExpireTime": {
        "name": "sourceExpireTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#FundInnerServiceMock#    success": {
        "name": "success",
        "type": "all",
        "datatype": "boolean",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      bidQuote": {
        "name": "bidQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      bizCode": {
        "name": "bizCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      clientBidQuote": {
        "name": "clientBidQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      clientMidQuote": {
        "name": "clientMidQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      clientOfferQuote": {
        "name": "clientMidQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      clientQuoteRef": {
        "name": "clientQuoteRef",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      currencyPair": {
        "name": "currencyPair",
        "type": "range",
        "datatype": "mixed",
        "extend": {
            "len": 2,
            "mode": [
                enums.currency,
                enums.currency
            ],
            "sep": [
                "/"
            ]
        }
    },
    "#IfxQuoteClientMock1#      currencyUnit": {
        "name": "currencyUnit",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      expiryTime": {
        "name": "expiryTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      generateTime": {
        "name": "generateTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      inst": {
        "name": "inst",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      isException": {
        "name": "isException",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      isLocked": {
        "name": "isLocked",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      isValid": {
        "name": "isValid",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      manualLockingTime": {
        "name": "manualLockingTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      memberCode": {
        "name": "memberCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      memo": {
        "name": "memo",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      midQuote": {
        "name": "midQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      nextValidRateTime": {
        "name": "nextValidRateTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      offerQuote": {
        "name": "offerQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      originQuoteRef": {
        "name": "originQuoteRef",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      pubStatus": {
        "name": "pubStatus",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      rcode": {
        "name": "rcode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      startTime": {
        "name": "startTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      taskId": {
        "name": "taskId",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      tenor": {
        "name": "tenor",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      thresholdTime": {
        "name": "thresholdTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      tradable": {
        "name": "tradable",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#      validTime": {
        "name": "validTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock1#    resultCode": {
        "name": "resultCode",
        "type": "range",
        "datatype": "enum",
        "extend": enums.IfxquoteResultCode
    },
    "#IfxQuoteClientMock1#    resultDesc": {
        "name": "resultDesc",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock1#    success": {
        "name": "success",
        "type": "all",
        "datatype": "boolean",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      baseCurrency": {
        "name": "baseCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#IfxQuoteClientMock2#        cent": {
        "name": "cent",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.centRange
    },
    "#IfxQuoteClientMock2#        currencyValue": {
        "name": "currencyValue",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.currencyValue
    },
    "#IfxQuoteClientMock2#          cent": {
        "name": "cent",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.centRange
    },
    "#IfxQuoteClientMock2#          currencyValue": {
        "name": "currencyValue",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.currencyValue
    },
    "#IfxQuoteClientMock2#      clientPrice": {
        "name": "midQuote",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      currencyUnit": {
        "name": "currencyUnit",
        "type": "all",
        "datatype": "int",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      expiryTime": {
        "name": "expiryTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      generateTime": {
        "name": "generateTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock2#            cent": {
        "name": "cent",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.centRange
    },
    "#IfxQuoteClientMock2#            currencyValue": {
        "name": "currencyValue",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.currencyValue
    },
    "#IfxQuoteClientMock2#              cent": {
        "name": "cent",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.centRange
    },
    "#IfxQuoteClientMock2#              currencyValue": {
        "name": "currencyValue",
        "type": "skip",
        "datatype": "enum",
        "extend": enums.currencyValue
    },
    "#IfxQuoteClientMock2#      price": {
        "name": "price",
        "type": "all",
        "datatype": "float",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      quoteCurrency": {
        "name": "quoteCurrency",
        "type": "range",
        "datatype": "enum",
        "extend": enums.currency
    },
    "#IfxQuoteClientMock2#      quoteDirection": {
        "name": "quoteDirection",
        "type": "range",
        "datatype": "enum",
        "extend": enums.QuoteDirectionEnum
    },
    "#IfxQuoteClientMock2#      quoteRef": {
        "name": "quoteRef",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock2#      startTime": {
        "name": "startTime",
        "type": "all",
        "datatype": "datetime",
        "extend": {}
    },
    "#IfxQuoteClientMock2#    resultCode": {
        "name": "resultCode",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock2#    resultDesc": {
        "name": "resultDesc",
        "type": "all",
        "datatype": "string",
        "extend": {}
    },
    "#IfxQuoteClientMock2#    success": {
        "name": "success",
        "type": "all",
        "datatype": "boolean",
        "extend": {}
    }
}
