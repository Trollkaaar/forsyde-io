package specs;

import com.fasterxml.jackson.annotation.*;

import java.util.Optional;


public class PortSpec {

    public String name;
    @JsonIdentityReference(alwaysAsId = true)
    public VertexTraitSpec vertexTrait;
    public Optional<Boolean> multiple = Optional.of(true);
    public Optional<Boolean> ordered = Optional.of(false);
    public PortDirection direction = PortDirection.BIDIRECTIONAL;

}
