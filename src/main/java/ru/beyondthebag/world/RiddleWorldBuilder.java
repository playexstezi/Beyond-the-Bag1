package ru.beyondthebag.world;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.beyondthebag.data.PocketData;
/** Deterministic three-stage color-riddle route. */
public final class RiddleWorldBuilder {
 private static final int Y=72, S=20;
 private static final BlockState[] COLORS={Blocks.RED_CONCRETE.defaultBlockState(),Blocks.BLUE_CONCRETE.defaultBlockState(),Blocks.YELLOW_CONCRETE.defaultBlockState()};
 private RiddleWorldBuilder(){}
 public static BlockPos buildIfNeeded(ServerLevel l,PocketData d,PocketData.Expedition e,int ox,int oz){
  if(!d.isMazeGenerated(e.runId())){
   for(int i=0;i<4;i++) room(l,ox+i*S,oz);
   for(int i=0;i<3;i++){
    int cx=ox+i*S,a=answer(e.seed(),i);
    set(l,new BlockPos(cx,Y+2,oz-4),COLORS[a]); set(l,new BlockPos(cx,Y+3,oz-4),COLORS[a]);
    for(int o=0;o<3;o++){BlockPos p=padPosition(ox,oz,i,o);set(l,p.below(),COLORS[o]);set(l,p,Blocks.STONE_PRESSURE_PLATE.defaultBlockState());}
   }
   set(l,gatePosition(ox,oz).below(),Blocks.PURPUR_BLOCK.defaultBlockState()); d.markMazeGenerated(e.runId());
  } return new BlockPos(ox,Y+1,oz);
 }
 public static int answer(long seed,int stage){long v=seed+stage*0x9E3779B97F4A7C15L;v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;v^=v>>>31;return Math.floorMod((int)(v>>>32),3);}
 public static BlockPos padPosition(int ox,int oz,int stage,int option){return new BlockPos(ox+stage*S+(option-1)*3,Y+1,oz+2);}
 public static BlockPos gatePosition(int ox,int oz){return new BlockPos(ox+3*S,Y+1,oz);}
 private static void room(ServerLevel l,int cx,int cz){
  for(int dx=-7;dx<=7;dx++)for(int dz=-6;dz<=6;dz++){
   set(l,new BlockPos(cx+dx,Y,cz+dz),Blocks.DEEPSLATE_TILES.defaultBlockState());boolean w=Math.abs(dx)==7||Math.abs(dz)==6;
   for(int dy=1;dy<=4;dy++)set(l,new BlockPos(cx+dx,Y+dy,cz+dz),w?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
   set(l,new BlockPos(cx+dx,Y+5,cz+dz),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
  }
  for(int dz=-1;dz<=1;dz++)for(int dy=1;dy<=3;dy++){set(l,new BlockPos(cx-7,Y+dy,cz+dz),Blocks.AIR.defaultBlockState());set(l,new BlockPos(cx+7,Y+dy,cz+dz),Blocks.AIR.defaultBlockState());}
  for(int x=cx+7;x<cx+S-7;x++)for(int dz=-2;dz<=2;dz++){
   set(l,new BlockPos(x,Y,cz+dz),Blocks.DEEPSLATE_TILES.defaultBlockState());
   for(int dy=1;dy<=4;dy++)set(l,new BlockPos(x,Y+dy,cz+dz),Math.abs(dz)==2?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
   set(l,new BlockPos(x,Y+5,cz+dz),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
  }
 }
 private static void set(ServerLevel l,BlockPos p,BlockState s){if(!l.getBlockState(p).equals(s))l.setBlock(p,s,3);}
}