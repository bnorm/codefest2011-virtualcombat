package deltasquad.virtual;

import java.io.Serializable;

import deltasquad.data.Drawable;
import deltasquad.data.Printable;


/**
 * The base class to all virtual objects. Used to make sure that all virtual objects are
 * <code>{@link java.lang.Cloneable Cloneable}</code>, <code>{@link java.io.Serializable Serializable}</code>,
 * <code>{@link deltasquad.data.Printable Printable}</code> and <code>{@link deltasquad.data.Drawable Drawable}</code>.
 * 
 * @author Brian Norman
 * @version 0.0.1 beta
 */
public abstract class VirtualObject implements Cloneable, Serializable, Printable, Drawable {

   /**
    * Determines if a deserialized file is compatible with this class.<br>
    * <br>
    * Maintainers must change this value if and only if the new version of this class is not compatible with old
    * versions.
    */
   private static final long serialVersionUID = 8764489217828783606L;
}
