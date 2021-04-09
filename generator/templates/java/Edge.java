package forsyde.io.java.types.edge;

import java.util.Optional;
{% if not type_data or not type_data['superTypes'] %}
import forsyde.io.java.core.Edge;
{%- endif %}
{% if type_data and 'required_ports' in type_data %}
import forsyde.io.java.core.Port;
import forsyde.io.java.core.ForSyDeModel;
{%- endif %}
import forsyde.io.java.core.Port;
import forsyde.io.java.core.Vertex;

{% if type_data and type_data['superTypes'] %}
public class {{type_name}} extends {{type_data['superTypes'][0]}} {
{% else %}
public class {{type_name}} extends Edge {
{% endif %}

	/**
	 * {@inheritDoc}
	 */
	public {{type_name}}(Vertex target, Vertex source) {
		super(target, source);
	}
	  
	  /**
	   * {@inheritDoc}
	   */
	  public {{type_name}}(Vertex source, Vertex target, Port sourcePort, Port targetPort) {
        super(target, source, sourcePort, targetPort);
	  }
	  
	  /**
	   * {@inheritDoc}
	   */
	  public {{type_name}}(Vertex source, Vertex target, Optional<Port> sourcePort, Optional<Port> targetPort) {
        super(target, source, sourcePort, targetPort);
	  }

    @Override
    public String getTypeName() {
        return "{{type_name}}";
    };

}
