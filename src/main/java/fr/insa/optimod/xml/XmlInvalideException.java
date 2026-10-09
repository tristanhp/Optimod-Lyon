package fr.insa.optimod.xml;

/**
 * Le fichier XML ne peut pas être utilisé : illisible, mal formé ou non
 * conforme au format attendu. Le message est destiné à l'utilisateur.
 */
public class XmlInvalideException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Crée l'exception.
     *
     * @param message explication destinée à l'utilisateur
     */
    public XmlInvalideException(String message) {
        super(message);
    }

    /**
     * Crée l'exception avec sa cause.
     *
     * @param message explication destinée à l'utilisateur
     * @param cause erreur d'origine
     */
    public XmlInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
