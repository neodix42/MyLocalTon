package org.ton.executors.validatorengineconsole;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class ValidatorEngineConsole {

    public void getStats(Node node) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "getstats");
        log.debug(result.getRight().get());
    }

    public void importF(Node node, String validatorIdHex) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "importf " + node.getTonDbKeyringDir() + validatorIdHex);
        log.debug(result.getRight().get());
    }

    public void changeFullNodeAddr(Node node, String newNodeKey) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "changefullnodeaddr " + newNodeKey);
        log.debug(result.getRight().get());
    }

    public void addValidatorAddr(Node node, String validatorIdHex, String newValAdnl, long electionEnd) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addvalidatoraddr  " + validatorIdHex + " " + newValAdnl + " " + electionEnd);
        log.debug(result.getRight().get());
    }

    public void addPermKey(Node node, String validatorIdHex, long electionId, long electionEnd) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addpermkey " + validatorIdHex + " " + electionId + " " + electionEnd);
        log.debug(result.getRight().get());
    }

    public void addTempKey(Node node, String validatorIdHex, long electionEnd) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addtempkey " + validatorIdHex + " " + validatorIdHex + " " + electionEnd);
        log.debug(result.getRight().get());
    }

    public void addAdnl(Node node, String newValAdnl) throws ExecutionException, InterruptedException {
        Pair<Process, Future<String>> result = new ValidatorEngineConsoleExecutor().execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addadnl " + newValAdnl + " 0");
        log.debug(result.getRight().get());
    }

    public String generateNewNodeKey(Node node) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "newkey");
        log.debug(result.getRight().get());

        return result.getRight().get().substring(result.getRight().get().length() - 65).trim();
    }

    public String exportPubKey(Node node, String newkey) throws ExecutionException, InterruptedException {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<Process, Future<String>> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "exportpub " + newkey);
        log.debug(result.getRight().get());

        return result.getRight().get().substring(result.getRight().get().indexOf("got public key:") + 15).trim();
    }
}
