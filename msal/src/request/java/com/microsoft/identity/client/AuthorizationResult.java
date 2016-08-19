//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.client;

import android.net.Uri;
import android.util.Base64;

import java.util.Map;

/**
 * MSAL internal response for handling the result from authorize endpoint.
 */
final class AuthorizationResult {

    private final String mAuthCode;
    private final AuthorizationStatus mAuthorizationStatus;
    private final String mError;
    private final String mSubError;

    private AuthorizationResult(final String authCode) {
        mAuthorizationStatus = AuthorizationStatus.SUCCESS;
        mAuthCode = authCode;

        mError = null;
        mSubError = null;
    }

    private AuthorizationResult(final AuthorizationStatus status, final String error, final String subError) {
        mAuthorizationStatus = status;
        mError = error;
        mSubError = subError;

        mAuthCode = null;
    }

    public static AuthorizationResult parseAuthorizationResponse(final String returnUri) {
        final Uri responseUri = Uri.parse(returnUri);
        final String result = responseUri.getQuery();

        final AuthorizationResult authorizationResult;
        if (MSALUtils.isEmpty(result)) {
            authorizationResult = getAuthorizationResultWithInvalidServerResponse();
        } else {
            final Map<String, String> urlParameters = MSALUtils.decodeUrlToMap(result, "&");
//            if (!urlParameters.containsKey("state")) {
//                authorizationResult = getAuthorizationResultWithInvalidServerResponse();
//            }else
            // TODO: append state
            if (urlParameters.containsKey(OauthConstants.TokenResponseClaim.CODE)) {
                authorizationResult = new AuthorizationResult(urlParameters.get(OauthConstants.Oauth2Parameters.CODE));
            } else if (urlParameters.containsKey(OauthConstants.Authorize.ERROR)) {
                final String error = urlParameters.get(OauthConstants.Authorize.ERROR);
                final String subError = urlParameters.get(OauthConstants.Authorize.ERROR_SUBCODE);

                // TODO: finalize the error handling.
                authorizationResult = new AuthorizationResult(AuthorizationStatus.PROTOCOL_ERROR, error,
                        subError);
            } else {
                authorizationResult = getAuthorizationResultWithInvalidServerResponse();
            }
        }

        return authorizationResult;
    }

    /**
     * @return The auth code in the redirect uri.
     */
    String getAuthCode() {
        return mAuthCode;
    }

    /**
     * @return The {@link AuthorizationStatus} indicating the auth status for the request sent to authorize endopoint.
     */
    AuthorizationStatus getAuthorizationStatus() {
        return mAuthorizationStatus;
    }

    /**
     * @return The error string in the query string of the redirect if applicable.
     */
    String getError() {
        return mError;
    }

    /**
     * @return The sub error code in the query string of the redirect if applicable.
     */
    String getSubError() {
        return mSubError;
    }

    /**
     * @return {@link AuthorizationResult} with invalid server response when the query string in redirect doesn't contain
     * either code or error.
     */
    static AuthorizationResult getAuthorizationResultWithInvalidServerResponse() {
        return new AuthorizationResult(AuthorizationStatus.UNKNOWN, Constants.MSALError.AUTHORIZATION_FAILED,
                Constants.MSALErrorMessage.AUTHORIZATION_SERVER_INVALID_RESPONSE);
    }

    /**
     * @return {@link AuthorizationResult} indicating that user cancels the flow. If user press the device back button or
     * user clicks on the cancel displayed in the browser.
     */
    static AuthorizationResult getAuthorizationResultWithUserCancel() {
        return new AuthorizationResult(AuthorizationStatus.USER_CANCEL, Constants.MSALError.USER_CANCEL,
                Constants.MSALErrorMessage.USER_CANCELLED_FLOW);
    }

    private String decodeState(final String encodedState) {
        if (MSALUtils.isEmpty(encodedState)) {
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING |Base64.URL_SAFE);
        return new String(stateBytes);
    }

    /**
     * Enum for representing different authorization status.
     */
    enum AuthorizationStatus {
        /**
         * Code is successfully returned.
         */
        SUCCESS,

        /**
         * User press device back button.
         */
        USER_CANCEL,

        /**
         * Returned URI contains error.
         */
        PROTOCOL_ERROR,

        INVALID_REQUEST,

        UNKNOWN
        //TODO:  Investigate how chrome tab returns http timeout error
    }
}
