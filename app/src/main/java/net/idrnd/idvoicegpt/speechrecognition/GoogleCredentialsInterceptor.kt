package net.idrnd.idvoicegpt.speechrecognition

import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Authenticates the gRPC channel using the specified [GoogleCredentials].
 */
class GoogleCredentialsInterceptor(private val credentials: Credentials) :
    ClientInterceptor {
    private var cachedMetadata: Metadata? = null
    private var lastMetadata: Map<String, List<String>>? = null
    override fun <RequestType, ResponseType> interceptCall(
        method: MethodDescriptor<RequestType, ResponseType>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<RequestType, ResponseType> {
        return object :
            ClientInterceptors.CheckedForwardingClientCall<RequestType, ResponseType>(
                next.newCall(method, callOptions)
            ) {
            @Throws(StatusException::class)
            override fun checkedStart(
                responseListener: Listener<ResponseType>,
                headers: Metadata
            ) {
                var cachedSaved: Metadata?
                val uri = serviceUri(next, method)
                synchronized(this) {
                    val latestMetadata =
                        getRequestMetadata(uri)
                    if (lastMetadata == null || lastMetadata !== latestMetadata) {
                        lastMetadata = latestMetadata
                        cachedMetadata = toHeaders(lastMetadata)
                    }
                    cachedSaved = cachedMetadata
                }
                headers.merge(cachedSaved)
                delegate().start(responseListener, headers)
            }
        }
    }

    /**
     * Generate a JWT-specific service URI. The URI is simply an identifier with enough
     * information for a service to know that the JWT was intended for it. The URI will
     * commonly be verified with a simple string equality check.
     */
    @Throws(StatusException::class)
    private fun serviceUri(channel: Channel, method: MethodDescriptor<*, *>): URI {
        val authority = channel.authority()
            ?: throw Status.UNAUTHENTICATED
                .withDescription("Channel has no authority")
                .asException()
        // Always use HTTPS, by definition.
        val scheme = "https"
        val defaultPort = 443
        val path = "/" + MethodDescriptor.extractFullServiceName(method.fullMethodName)
        var uri: URI
        uri = try {
            URI(scheme, authority, path, null, null)
        } catch (e: URISyntaxException) {
            throw Status.UNAUTHENTICATED
                .withDescription("Unable to construct service URI for auth")
                .withCause(e).asException()
        }
        // The default port must not be present. Alternative ports should be present.
        if (uri.port == defaultPort) {
            uri = removePort(uri)
        }
        return uri
    }

    @Throws(StatusException::class)
    private fun removePort(uri: URI): URI {
        return try {
            URI(
                uri.scheme,
                uri.userInfo,
                uri.host,
                -1 /* port */,
                uri.path,
                uri.query,
                uri.fragment
            )
        } catch (e: URISyntaxException) {
            throw Status.UNAUTHENTICATED
                .withDescription("Unable to construct service URI after removing port")
                .withCause(e).asException()
        }
    }

    @Throws(StatusException::class)
    private fun getRequestMetadata(uri: URI): Map<String, List<String>> {
        return try {
            credentials.getRequestMetadata(uri)
        } catch (e: IOException) {
            throw Status.UNAUTHENTICATED.withCause(e).asException()
        }
    }

    companion object {
        private fun toHeaders(metadata: Map<String, List<String>>?): Metadata {
            val headers = Metadata()
            if (metadata == null) {
                return headers
            }
            for (key in metadata.keys) {
                val headerKey = Metadata.Key.of(
                    key,
                    Metadata.ASCII_STRING_MARSHALLER
                )
                for (value in metadata[key]!!) {
                    headers.put(headerKey, value)
                }
            }
            return headers
        }
    }
}
