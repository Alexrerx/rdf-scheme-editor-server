package org.eclipse.rdf4j.http.server.transaction;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(service = TransactionControllerRollback.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerRollback {

    private static Logger logger = LoggerFactory.getLogger(TransactionControllerRollback.class.getName());

    public TransactionControllerRollback() {
        System.out.println("Init TransactionControllerRollback");
    }

    @DELETE
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId) throws Exception {

        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                transactionId);

        if (connection == null) {
            logger.warn("could not find connection for transaction id {}", transactionId);
            throw
                    new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
        }
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        try {
            if (action.equals(Action.ROLLBACK)) {
                logger.info("transaction rollback");
                try {
                    connection.rollback();
                } finally {
                    ActiveTransactionRegistry.INSTANCE.deregister(transactionId);
                    connection.close();
                }
                logger.info("transaction rollback request finished.");
            } else {
                throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED);
            }
        } finally {
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(transactionId);
        }
    }

}