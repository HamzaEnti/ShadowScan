package controller;

import model.ResultatHost;

/**
 * Listener que rep notificacions quan un host és descobert.
 *
 * Substitueix la dependència directa de ScanTask cap a view.MainFrame,
 * trencant el cicle controller → view i complint amb MVC.
 *
 * La implementació de la UI hauria d'embolicar les actualitzacions
 * Swing dins SwingUtilities.invokeLater.
 */
public interface HostFoundListener {
    void onHostFound(ResultatHost host);
}
