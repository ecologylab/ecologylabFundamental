package ecologylab.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation.
 * S.IM.PL Serialization transparency. 
 * Enables overriding the class used for the ClassDescriptor for a particular type.
 * 
 * @author andruid
 */

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

public @interface serial_class_descriptor
{
  Class value();
}
