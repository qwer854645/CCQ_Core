"""Rebuilt armored entry with two independent, concealed Create sliding gates."""
import nbtlib as n

def install_gate(put,box,clear,entities,label,z,base):
    left=45 if label=='vault' else 46
    travel=51-left
    clear(left-travel,base,z,left-1,base+4,z)
    # Keep the complete hanger sweep empty. Fixed fascia is one plane ahead
    # and behind the moving leaf, so a cycle cannot eat its lintel away.
    clear(left-travel,base+3,z,51,base+4,z)
    # The leaf needs a three-high pocket; only the far end of the hanger reaches
    # above it. Fill the unused upper corners instead of clearing a five-high bay.
    box(left-travel,base+3,z,49-travel,base+4,z,'light_gray_concrete')
    put(51,base+3,z,'light_gray_concrete')
    for zz in [z-1,z+1]:
        box(left-1,base+3,zz,51,base+4,zz,'light_gray_concrete')
        box(left-1,base+3,zz,51,base+3,zz,'polished_deepslate')
    box(left,base,z,50,base+2,z,'createbigcannons:steel_block')
    for y in [base,base+2]:box(left,y,z,50,y,z,'createbigcannons:nethersteel_block')
    for x in [left,50]:put(x,base+1,z,'createbigcannons:nethersteel_block')
    box(50,base+3,z,50,base+4,z,'createbigcannons:steel_block')
    put(51,base+4,z,'createbigcannons:steel_block')
    put(52,base+4,z,'create:sticky_mechanical_piston',facing='west',axis_along_first=False,state='retracted')
    for x in range(53,53+travel):put(x,base+4,z,'create:piston_extension_pole',facing='west',axis_along_first=False)
    for side in [-1,1]:
        control=z+2*side;column=z+side
        put(51,base+1,control,'create:hand_crank',facing='west',waterlogged=False)
        put(52,base+1,control,'create:gearbox',axis='y')
        put(52,base+1,column,'create:gearbox',axis='x')
        for y in [base+2,base+3]:put(52,y,column,'create:shaft',axis='y')
        put(52,base+4,column,'create:gearbox',axis='x')
    for zz in [z-1,z+1]:
        box(left,base-1,zz,50,base-1,zz,'polished_deepslate')
        for x in [left,50]:put(x,base-1,zz,'create:industrial_iron_block')
    for x,y,X,Y in [(left,base,51,base+3),(50,base+2,51,base+5),(50,base+4,52,base+5)]:
        cx=(x+X)/2;half=(X-x)/2
        entities.append(n.Compound({'pos':n.List[n.Double]([cx,float(y),z+0.5]),'blockPos':n.List[n.Int]([int(cx),y,z]),
            'nbt':n.Compound({'id':n.String('create:super_glue'),'Pos':n.List[n.Double]([cx,float(y),z+0.5]),
                'Motion':n.List[n.Double]([0.,0.,0.]),'Rotation':n.List[n.Float]([0.,0.]),
                'From':n.List[n.Double]([-half,0.,-0.5]),'To':n.List[n.Double]([half,float(Y-y),0.5])})}))
    return {'name':label,'piston':[52,base+4,z],'leaf':[left,base,z,50,base+2,z],'travel':travel,
        'controls':[[51,base+1,z-2],[51,base+1,z+2]]}

def build_vault(put,box,clear,entities):
    # Bulkhead at the stair landing: no route around its sides.
    box(36,1,48,59,1,54,'deepslate_tiles')
    box(36,2,48,44,6,54,'light_gray_concrete')
    box(51,2,48,59,6,53,'light_gray_concrete')
    # Kinetic blocks and poles occupy their exact authored cells in the shell.
    box(51,2,54,59,6,54,'light_gray_concrete')
    clear(45,2,48,50,6,54)
    box(45,5,48,50,5,54,'light_gray_concrete')
    box(36,5,48,59,6,48,'gray_concrete')
    box(36,7,48,59,7,54,'polished_andesite')
    for z in [48,54]:
        for x in [47,48]:put(x,5,z,'sea_lantern')
    # Seal the complete original eight-wide landing, including the west flank.
    for x in [36,59]:box(x,2,48,x,6,54,'polished_deepslate')
    for z in [48,54]:box(36,7,z,59,7,z,'polished_deepslate')
    return install_gate(put,box,clear,entities,'vault',51,2)

def build(put,box,clear,entities):
    # Entire entrance is centered on the building's x=47.5 centerline.
    clear(34,9,76,61,23,95)
    box(34,0,76,61,8,95,'gray_concrete')
    box(34,8,76,61,8,95,'polished_andesite')
    box(34,9,76,43,22,95,'light_gray_concrete')
    box(52,9,76,61,22,95,'light_gray_concrete')
    box(44,15,76,51,22,95,'light_gray_concrete')
    box(34,23,76,61,23,95,'polished_deepslate')
    clear(44,9,76,51,14,95)
    # Continuous mirrored jambs and deep lintels, tied into the original hull.
    for z in [76,84,94,95]:
        for x in [42,43,52,53]:box(x,9,z,x,15,z,'polished_deepslate')
        box(42,15,z,53,16,z,'polished_deepslate')
    for z in range(77,95,4):
        for x in [45,50]:put(x,15,z,'sea_lantern')
    result=[]
    def glue(x,y,z,X,Y):
        center=(x+X+1)/2
        entities.append(n.Compound({'pos':n.List[n.Double]([center,float(y),z+.5]),'blockPos':n.List[n.Int]([int(center),y,z]),
            'nbt':n.Compound({'id':n.String('create:super_glue'),'Pos':n.List[n.Double]([center,float(y),z+.5]),
                'Motion':n.List[n.Double]([0.,0.,0.]),'Rotation':n.List[n.Float]([0.,0.]),
                'From':n.List[n.Double]([x-center,0.,-.5]),'To':n.List[n.Double]([X+1-center,float(Y+1-y),.5])})}))
    for label,z in [('inner',79),('outer',89)]:
        # Pockets and exact hanger sweeps; fixed fascia is outside these planes.
        clear(40,9,z,55,14,z)
        clear(40,15,z,44,18,z);clear(53,15,z,57,19,z);clear(51,14,z,57,14,z)
        clear(40,18,z,47,18,z);clear(48,19,z,57,19,z)
        for left,right,post,top in [(44,47,44,18),(48,51,53,19)]:
            box(left,9,z,right,14,z,'createbigcannons:steel_block')
            for y in [9,14]:box(left,y,z,right,y,z,'createbigcannons:nethersteel_block')
            edge=44 if left==44 else 51
            box(edge,9,z,edge,14,z,'createbigcannons:nethersteel_block')
            if post>right:box(right,14,z,post,14,z,'createbigcannons:steel_block')
            box(post,15,z,post,top,z,'createbigcannons:steel_block')
            box(left,top,z,max(right,post),top,z,'createbigcannons:steel_block')
            glue(left,9,z,max(right,post),14);glue(post,14,z,post,top);glue(left,top,z,max(right,post),top)
        for x,y,facing,polerange in [(48,18,'west',range(49,53)),(47,19,'east',range(43,47))]:
            put(x,y,z,'create:sticky_mechanical_piston',facing=facing,axis_along_first=False,state='retracted')
            for px in polerange:put(px,y,z,'create:piston_extension_pole',facing=facing)
        # Common drive from either side operates both leaves; each gate is independent.
        for side in [-1,1]:
            zz=z+side
            put(52,10,z+side*2,'create:hand_crank',facing='west',waterlogged=False)
            for x in range(53,58):put(x,10,z+side*2,'create:shaft',axis='x')
            put(58,10,z+side*2,'create:gearbox',axis='y')
            put(58,10,zz,'create:gearbox',axis='x')
            for y in range(11,20):put(58,y,zz,'create:shaft',axis='y')
            put(58,16,zz,'create:gearbox',axis='x')
        put(58,16,z,'create:gearbox',axis='x')
        for y,side,end in [(18,1,48),(19,-1,47)]:
            zz=z+side;put(58,y,zz,'create:gearbox',axis='z')
            for x in range(end+1,58):put(x,y,zz,'create:shaft',axis='x')
            put(end,y,zz,'create:gearbox',axis='y')
        result.append({'name':label,'pistons':[[48,18,z],[47,19,z]],'leaf':[44,9,z,51,14,z],'travel':4,'controls':[[52,10,z-2],[52,10,z+2]]})
    return result
