package cz.cuni.mff.d3s.trupple.language.nodes.builtin;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import cz.cuni.mff.d3s.trupple.language.customvalues.PascalArray;

@NodeInfo(shortName = "map")
public abstract class MapArrayNode extends BuiltinNode {

    @Specialization
    public PascalArray map(PascalArray array) {
        // do something with the array
        PascalArray createCopy = array.createCopy();
        return createCopy;
    }

}
