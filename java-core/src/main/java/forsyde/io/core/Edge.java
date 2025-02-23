/**
 * 
 */
package forsyde.io.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjordao
 *
 *         Class containing all information for an Edge.
 * 
 *         The edge contains references to the source and target
 *         {@link Vertex}es as well as the {@link Port}s being connect on both
 *         ends, in case they exist. The edges also have types associated with
 *         them so that extra deductions can be made along the EDA flow.
 *
 *         Deprecated in favor of {@link EdgeInfo}
 */
@Deprecated
final public class Edge {

	public Vertex source;
	public Vertex target;
	public Optional<String> sourcePort;
	public Optional<String> targetPort;
	public Set<Trait> edgeTraits = new HashSet<Trait>();

	/**
	 * Utility constructor wrapping the source and target ports into empty optionals
	 * for constructor.
	 * 
	 * @param target Target Vertex for this edge.
	 * @param source Source vertex for this edge.
	 */
	public Edge(Vertex source, Vertex target) {
		this.target = target;
		this.source = source;
		this.sourcePort = Optional.empty();
		this.targetPort = Optional.empty();
	}

	/**
	 * Utility constructor wrapping the source and target ports into optionals for
	 * constructor.
	 * 
	 * @param target     Target Vertex for this edge.
	 * @param source     Source vertex for this edge.
	 * @param targetPort target vertex port for this edge.
	 * @param sourcePort source vertex port for this edge.
	 */
	public Edge(Vertex source, Vertex target, String sourcePort, String targetPort) {
		this.target = target;
		this.source = source;
		this.targetPort = targetPort == null ? Optional.empty() : Optional.of(targetPort);
		this.sourcePort = sourcePort == null ? Optional.empty() : Optional.of(sourcePort);
	}

	/**
	 * Complete constructor for the {@link Edge} class, passing the optional ports
	 * that it might contain as well as the references for the source and target
	 * vertexes.
	 * 
	 * @param target     Target Vertex for this edge.
	 * @param source     Source vertex for this edge.
	 * @param targetPort {@link Optional} target vertex port for this edge.
	 * @param sourcePort {@link Optional} source vertex port for this edge.
	 */
	public Edge(Vertex source, Vertex target, Optional<String> sourcePort,
			Optional<String> targetPort) {
		this.target = target;
		this.source = source;
		this.targetPort = targetPort;
		this.sourcePort = sourcePort;
	}

	public Set<Trait> getTraits() {
		return edgeTraits;
	}

	public boolean hasTrait(EdgeTrait trait) {
		return edgeTraits.stream().anyMatch(t -> t.refines(trait));
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, targetPort, source, sourcePort);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Edge)) {
			return false;
		}
		Edge other = (Edge) obj;
		return Objects.equals(target, other.target) && Objects.equals(targetPort, other.targetPort)
				&& Objects.equals(source, other.source) && Objects.equals(sourcePort, other.sourcePort);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Edge [traits=").append(edgeTraits).append(", source=").append(source).append(", target=").append(target)
				.append(", sourcePort=").append(sourcePort).append(", targetPort=").append(targetPort).append("]");
		return builder.toString();
	}

	public String toIDString() {
		StringBuilder builder = new StringBuilder();
		builder.append(source.getIdentifier());
		sourcePort.ifPresent(s -> builder.append(".").append(s));
		builder.append("[").append(
				edgeTraits.stream().map(Trait::getName).collect(Collectors.joining(";"))
		).append("]");
		targetPort.ifPresent(s -> builder.append(".").append(s));
		builder.append(target.getIdentifier());
		return builder.toString();
	}

	public String toCppCompileableIDString() {
		StringBuilder builder = new StringBuilder();
		builder.append(source.getIdentifier());
		sourcePort.ifPresent(s -> builder.append("_").append(s));
		builder.append("_to_");
		targetPort.ifPresent(s -> builder.append("_").append(s));
		builder.append(target.getIdentifier());
		builder.append("_with_").append(
				edgeTraits.stream().map(Trait::getName).collect(Collectors.joining("_"))
		);
		return builder.toString();
	}

	public Vertex getSource() {
		return source;
	}

	public Vertex getTarget() {
		return target;
	}

	public Optional<String> getSourcePort() {
		return sourcePort;
	}

	public Optional<String> getTargetPort() {
		return targetPort;
	}

	public void addTraits(Trait... traits) {
		edgeTraits.addAll(Arrays.asList(traits.clone()));
	}

}
