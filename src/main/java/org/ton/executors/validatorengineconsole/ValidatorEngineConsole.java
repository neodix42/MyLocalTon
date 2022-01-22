package org.ton.executors.validatorengineconsole;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.io.File;

@Slf4j
public class ValidatorEngineConsole {

    public void getStats(Node node) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "getstats");
        log.debug(result.getLeft());
    }

    public void importF(Node node, String validatorIdHex) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "importf " + node.getTonDbKeyringDir() + validatorIdHex);
        log.debug(result.getLeft());
    }

    public void changeFullNodeAddr(Node node, String newNodeKey) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "changefullnodeaddr " + newNodeKey);
        log.debug(result.getLeft());
    }

    public void addValidatorAddr(Node node, String validatorIdHex, String newValAdnl, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addvalidatoraddr  " + validatorIdHex + " " + newValAdnl + " " + (electionId + electedForDuration + 1)); // TODO some bug here
        log.debug(result.getLeft());
    }

    public void addPermKey(Node node, String validatorIdHex, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addpermkey " + validatorIdHex + " " + electionId + " " + (electionId + electedForDuration + 1));
        log.debug(result.getLeft());
    }

    public void addTempKey(Node node, String validatorIdHex, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addtempkey " + validatorIdHex + " " + validatorIdHex + " " + (electionId + electedForDuration + 1));
        log.debug(result.getLeft());
    }

    public void addAdnl(Node node, String newValAdnl) {
        Pair<String, Process> result = new ValidatorEngineConsoleExecutor().execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addadnl " + newValAdnl + " 0");
        log.debug(result.getLeft());
    }

    public String generateNewNodeKey(Node node) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "newkey");
        log.debug(result.getLeft());

        return result.getLeft().substring(result.getLeft().length() - 65).trim();
    }

    public String exportPubKey(Node node, String newkey) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "exportpub " + newkey);
        log.debug(result.getLeft());

        return result.getLeft().substring(result.getLeft().indexOf("got public key:") + 15).trim();
    }
}
