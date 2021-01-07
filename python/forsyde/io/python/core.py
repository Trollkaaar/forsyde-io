from dataclasses import dataclass, field
from typing import Optional, Set, Any, Dict, Tuple

_port_id_counter = 1
_vertex_id_counter = 1


class Type(object):

    """This class represents any type defined in the 'type' hierarchy."""

    def __init__(self):
        """TODO: to be defined.
        """
        pass

    def __hash__(self):
        return hash(self.get_type_name())

    def is_refinement(self, other: "Type") -> bool:
        return False

    def get_type_name(self) -> str:
        return "Unknown"

    def get_required_ports(self) -> Dict[str, Optional[str]]:
        return dict()

    def get_required_properties(self) -> Dict[str, Any]:
        return dict()


@dataclass
class Port(object):

    """Docstring for Port. """

    identifier: Optional[str] = field(default=None, hash=True)
    port_type: Type = field(default=Type(), compare=False, hash=False)

    def __post_init__(self):
        global _port_id_counter
        if not self.identifier:
            self.identifier = f"port{_port_id_counter}"
            _port_id_counter += 1

    def __hash__(self):
        return hash(self.identifier)


@dataclass
class Vertex(object):

    """Docstring for Vertex. """

    identifier: Optional[str] = field(default=None, hash=True)
    ports: Set[Port] = field(
        default_factory=lambda: set(),
        compare=False,
        hash=False)
    properties: Dict[str, Any] = field(
        default_factory=lambda: dict(),
        compare=False,
        hash=False)
    vertex_type: Type = field(
        default=Type(),
        compare=False,
        hash=False)

    def __post_init__(self):
        global _vertex_id_counter
        if not self.identifier:
            self.identifier = f"vertex{_vertex_id_counter}"
            _vertex_id_counter += 1

    def __hash__(self):
        return hash(self.identifier)


@dataclass
class Edge(object):

    """Docstring for Edge. """
    source_vertex: Vertex
    target_vertex: Vertex
    source_vertex_port: Optional[Port] = field(default=None) 
    target_vertex_port: Optional[Port] = field(default=None)
    edge_type: Type = field(default=Type(), compare=False)

    def __hash__(self):
        return hash(
            (self.source_vertex, self.target_vertex)
        )

    def ids_tuple(self):
        return (
            self.source_vertex.identifier,
            self.target_vertex.identifier,
            self.source_vertex_port.identifier if
            self.source_vertex_port else None,
            self.target_vertex_port.identifier if
            self.target_vertex_port else None,
            self.edge_type.get_type_name()
        )
