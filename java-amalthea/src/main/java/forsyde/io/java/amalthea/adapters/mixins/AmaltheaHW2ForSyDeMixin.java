package forsyde.io.java.amalthea.adapters.mixins;

import forsyde.io.core.*;
import forsyde.io.java.adapters.EquivalenceModel2ModelMixin;
import forsyde.io.java.core.*;
import forsyde.io.java.typed.viewers.decision.Allocated;
import forsyde.io.java.typed.viewers.platform.*;
import forsyde.io.java.typed.viewers.platform.runtime.RoundRobinScheduler;
import forsyde.io.java.typed.viewers.visualization.GreyBox;
import forsyde.io.java.typed.viewers.visualization.Visualizable;
import org.eclipse.app4mc.amalthea.model.*;

import java.util.*;
import java.util.stream.Collectors;

public interface AmaltheaHW2ForSyDeMixin extends EquivalenceModel2ModelMixin<INamed, Vertex> {

    default void fromHWtoForSyDe(Amalthea amalthea, SystemGraph systemGraph) {
        if (amalthea.getHwModel() != null) {
            amalthea.getHwModel().getStructures().forEach(hwStructure -> {
                fromStructureToVertex(systemGraph, hwStructure);
                fromStructureToEdges(systemGraph, hwStructure);
            });
            connectModulesBetweenContainers(amalthea, systemGraph);
        }
    }

    default void fromPUIntoVertex(ProcessingUnit pu, Vertex v) {
        ProcessingUnitDefinition puDef = pu.getDefinition();
        v.addTraits(VertexTrait.PLATFORM_INSTRUMENTEDPROCESSINGMODULE);
        InstrumentedProcessingModule profPu = InstrumentedProcessingModule.safeCast(v).get();
        Map<String, Map<String, Double>> provisions = new HashMap<>();
        Map<String, Double> provisionsInner = new HashMap<>();
        for (HwFeature feature : puDef.getFeatures()) {
            final HwFeatureCategory hwFeatureCategory = feature.getContainingCategory();
            if (feature.getName().contains("instructionsPerCycle")
                    || feature.getName().contains("InstructionsPerCycle")
                    || feature.getName().contains("IPC")) {
                provisionsInner.put(hwFeatureCategory.getName(), feature.getValue());
            }
        }
        provisions.put("defaultNeeds", provisionsInner);
        provisions.put("defaultTicks", Map.of("tick", 1.0));
        profPu.setModalInstructionsPerCycle(provisions);
    }

    default void fromCUIntoVertex(SystemGraph systemGraph, ConnectionHandler connectionHandler, Vertex connectionHandlerVertex) {
        switch (connectionHandler.getDefinition().getPolicy()) {
            case ROUND_ROBIN:
                final RoundRobinScheduler roundRobinScheduler = RoundRobinScheduler.enforce(systemGraph.newVertex(connectionHandlerVertex.getIdentifier() + "Scheduler"));
                final RoundRobinCommunicationModule roundRobinCommunicationModule = RoundRobinCommunicationModule.enforce(connectionHandlerVertex);
                final List<HwConnection> outgoingConnections = connectionHandler.getPorts().stream().flatMap(p -> p.getConnections().stream())
                        .filter(p -> p.getPort1().getNamedContainer().equals(connectionHandler)).collect(Collectors.toList());
                final List<HwConnection> incomingConnections = connectionHandler.getPorts().stream().flatMap(p -> p.getConnections().stream())
                        .filter(p -> p.getPort2().getNamedContainer().equals(connectionHandler)).collect(Collectors.toList());
                HashMap<String, Integer> allocation = new HashMap<>();
                outgoingConnections.forEach(p -> allocation.put(p.getPort2().getNamedContainer().getName(), 1));
                incomingConnections.forEach(p -> allocation.put(p.getPort1().getNamedContainer().getName(), 1));
                roundRobinCommunicationModule.setAllocatedWeights(allocation);
                roundRobinCommunicationModule.setTotalWeights(allocation.size());
                addEquivalence(connectionHandler, roundRobinScheduler.getViewedVertex());
                Allocated.enforce(roundRobinScheduler).insertAllocationHostsPort(systemGraph, roundRobinCommunicationModule);
                GreyBox.enforce(connectionHandlerVertex).insertContainedPort(systemGraph, Visualizable.enforce(roundRobinScheduler));
//                forSyDeSystemGraph.connect(v, rrVertex, "contained", EdgeTrait.VISUALIZATION_VISUALCONTAINMENT);
                break;
            default:
                break;
        }
    }

    default void fromStructureToVertex(SystemGraph model, HwStructure structure) {
        fromStructureToVertex(model, structure, "");
    }

    default void fromStructureToVertex(SystemGraph model, HwStructure structure,
                                       String prefix) {
        final AbstractStructure abstractStructure = AbstractStructure.enforce(model.newVertex(prefix + structure.getName()));
//        final Vertex structureVertex = new Vertex(prefix + structure.getName(), VertexTrait.PLATFORM_ABSTRACTSTRUCTURE);
        final GreyBox structureGreyBox = GreyBox.enforce(abstractStructure);
        addEquivalence(structure, abstractStructure.getViewedVertex());
        for (HwStructure childStructure : structure.getStructures()) {
            fromStructureToVertex(model, childStructure, prefix + structure.getName() + ".");
            equivalent(childStructure).flatMap(PlatformElem::safeCast).ifPresent(childStructureVertex -> {
                abstractStructure.insertSubmodulesPort(model, childStructureVertex);
                structureGreyBox.insertContainedPort(model, Visualizable.enforce(childStructureVertex));
            });
        }
        for (HwModule module : structure.getModules()) {
            final Vertex moduleVertex = new Vertex(prefix + structure.getName() + "." + module.getName());
            model.addVertex(moduleVertex);
            final DigitalModule digitalModule = DigitalModule.enforce(moduleVertex);
            final Visualizable visualizableModule = Visualizable.enforce(moduleVertex);
            digitalModule.setOperatingFrequencyInHertz(fromFrequencyToLong(module.getFrequencyDomain().getDefaultValue()));
            addEquivalence(module, moduleVertex);
            for (HwPort port : module.getPorts()) {
                moduleVertex.addPort(port.getName());
            }
            if (module instanceof ProcessingUnit) {
                ProcessingUnit processingUnit = (ProcessingUnit) module;
                moduleVertex.addTraits(VertexTrait.PLATFORM_GENERICPROCESSINGMODULE);
                fromPUIntoVertex(processingUnit, moduleVertex);
            } else if (module instanceof Memory) {
                Memory memory = (Memory) module;
                final GenericMemoryModule genericMemoryModule = GenericMemoryModule.enforce(moduleVertex);
                genericMemoryModule.setSpaceInBits(memory.getDefinition().getSize().getNumberBits());
                //moduleVertex.putProperty("max_memory_in_bits", memory.getDefinition().getSize().getNumberBits());
            } else if (module instanceof Cache) {
                Cache cache = (Cache) module;
                moduleVertex.addTraits(VertexTrait.PLATFORM_GENERICCACHEMODULE);
            } else if (module instanceof ConnectionHandler) {
                ConnectionHandler connectionHandler = (ConnectionHandler) module;
                moduleVertex.addTraits(VertexTrait.PLATFORM_INSTRUMENTEDCOMMUNICATIONMODULE);
                final InstrumentedCommunicationModule interconnectVertex = new InstrumentedCommunicationModuleViewer(
                        moduleVertex);
                interconnectVertex.setMaxCyclesPerFlit(1);
                interconnectVertex.setMaxConcurrentFlits(connectionHandler.getDefinition().getMaxConcurrentTransfers());
                // burst size is always in B, it seems
                interconnectVertex
                        .setFlitSizeInBits((long) connectionHandler.getDefinition().getMaxBurstSize() * 8L);
                interconnectVertex.setInitialLatency(
                        connectionHandler.getDefinition().getReadLatency() != null ?
                        connectionHandler.getDefinition().getReadLatency().getUpperBound() :
                        0L);
                fromCUIntoVertex(model, connectionHandler, moduleVertex);
            }
            model.connect(abstractStructure.getViewedVertex(), moduleVertex, "submodules", EdgeTrait.PLATFORM_STRUCTURALCONNECTION);
            model.connect(abstractStructure.getViewedVertex(), moduleVertex, "contained", EdgeTrait.VISUALIZATION_VISUALCONTAINMENT);
        }
        for (HwPort port : structure.getPorts()) {
            abstractStructure.getPorts().add(port.getName());
        }
    }

    default void fromStructureToEdges(SystemGraph model, HwStructure structure) {
        for (HwStructure childStructure : structure.getStructures()) {
            fromStructureToEdges(model, childStructure);
        }
        for (HwConnection connection : structure.getConnections()) {
            equivalents(connection.getPort1().getNamedContainer()).forEach(sourceVertex -> {
                equivalents(connection.getPort2().getNamedContainer()).forEach(targetVertex -> {
                    if (DigitalModule.conforms(sourceVertex) && DigitalModule.conforms(targetVertex)) {
                        model.connect(sourceVertex, targetVertex, connection.getPort1().getName(), connection.getPort2().getName(), EdgeTrait.PLATFORM_PHYSICALCONNECTION);
                        model.connect(sourceVertex, targetVertex, connection.getPort1().getName(), connection.getPort2().getName(), EdgeTrait.VISUALIZATION_VISUALCONNECTION);
                        // add the port information
                        if (!SynthetizableDigitalPorts.conforms(sourceVertex)) {
                            SynthetizableDigitalPorts.enforce(sourceVertex);
                            // add the properties if they dont exist.
                            SynthetizableDigitalPorts.safeCast(sourceVertex).ifPresent(source -> {
                                source.setPortProtocolAcronym(new HashMap<>());
                                source.setPortIsInitiator(new HashMap<>());
                                source.setPortWidthInBits(new HashMap<>());
                            });
                        }
                        if (!SynthetizableDigitalPorts.conforms(targetVertex)) {
                            // add the properties if they dont exist.
                            SynthetizableDigitalPorts.enforce(targetVertex);
                            SynthetizableDigitalPorts.safeCast(targetVertex).ifPresent(target -> {
                                target.setPortProtocolAcronym(new HashMap<>());
                                target.setPortIsInitiator(new HashMap<>());
                                target.setPortWidthInBits(new HashMap<>());
                            });
                        }
                        SynthetizableDigitalPorts.safeCast(sourceVertex).ifPresent(source -> {
                            source.getPortProtocolAcronym().put(connection.getPort1().getName(), connection.getPort1().getPortInterface().getLiteral());
                            source.getPortWidthInBits().put(connection.getPort1().getName(), connection.getPort1().getBitWidth());
                            source.getPortIsInitiator().put(connection.getPort1().getName(), connection.getPort1().getPortType().equals(PortType.INITIATOR));
                        });
                        SynthetizableDigitalPorts.safeCast(targetVertex).ifPresent(target -> {
                            target.getPortProtocolAcronym().put(connection.getPort2().getName(), connection.getPort2().getPortInterface().getLiteral());
                            target.getPortWidthInBits().put(connection.getPort2().getName(), connection.getPort2().getBitWidth());
                            target.getPortIsInitiator().put(connection.getPort2().getName(), connection.getPort2().getPortType().equals(PortType.INITIATOR));
                        });
                    }
                });
            });
        }
    }

    default void connectModulesBetweenContainers(Amalthea amalthea, SystemGraph systemGraph) {
        systemGraph.vertexSet().stream()
        .filter(AbstractStructure::conforms)
        .map(v -> AbstractStructure.safeCast(v).get())
        .forEach(abstractStructure -> {
            for (EdgeInfo inInfo : systemGraph.incomingEdgesOf(abstractStructure.getViewedVertex())) {
                final Vertex inVertex = systemGraph.getEdgeSource(inInfo);
                for (EdgeInfo outInfo : systemGraph.outgoingEdgesOf(abstractStructure.getViewedVertex())) {
                    final Vertex outVertex = systemGraph.getEdgeTarget(outInfo);
                    if (inInfo.getTargetPort().equals(outInfo.getSourcePort()) && !AbstractStructure.conforms(inVertex)
                            && !AbstractStructure.conforms(outVertex)) {
                        final EdgeInfo edgeInfo = new EdgeInfo(inVertex.identifier, outVertex.identifier, inInfo.getTargetPort(), outInfo.getSourcePort());
                        edgeInfo.edgeTraits.addAll(inInfo.edgeTraits);
                        edgeInfo.edgeTraits.addAll(outInfo.edgeTraits);
                        systemGraph.addEdge(inVertex, outVertex, edgeInfo);
                    }
                }
            }
        });
    }

    default Long fromFrequencyToLong(Frequency freq) {
        double multiplier = 1.0;
        switch (freq.getUnit()) {
            case HZ:
                multiplier = 1.0;
                break;
            case KHZ:
                multiplier = 1000.0;
                break;
            case MHZ:
                multiplier = 1000000.0;
                break;
            case GHZ:
                multiplier = 1000000000.0;
                break;
            default:
                break;
        }
        return Double.valueOf(freq.getValue() * multiplier).longValue();
    }

}
