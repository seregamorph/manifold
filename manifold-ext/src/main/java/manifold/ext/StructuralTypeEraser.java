package manifold.ext;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import java.util.ArrayList;
import javax.lang.model.type.NoType;

/**
 */
class StructuralTypeEraser extends Types.UnaryVisitor<Type>
{
  private ExtensionTransformer _extensionTransformer;
  Types _types;

  public StructuralTypeEraser( ExtensionTransformer extensionTransformer )
  {
    _extensionTransformer = extensionTransformer;
    _types = Types.instance( _extensionTransformer.getTypeProcessor().getContext() );
  }

  @Override
  public Type visitClassType( Type.ClassType t, Void s )
  {
    boolean erased = false;
    Type erasure = _types.erasure( t );
    Type base = visitType( erasure, s );
    if( base != erasure )
    {
      erased = true;
    }
    ArrayList<Type> params = new ArrayList<>();
    for( Type arg : t.allparams() )
    {
      Type param = visit( arg );
      params.add( param );
      if( param != arg )
      {
        erased = true;
      }
    }
    if( erased )
    {
      return new Type.ClassType( t.getEnclosingType(), List.from( params ), base.tsym );
    }
    return t;
  }

  @Override
  public Type visitArrayType( Type.ArrayType t, Void aVoid )
  {
    Type compType = visit( t.getComponentType() );
    if( compType == t.getComponentType() )
    {
      return t;
    }
    return new Type.ArrayType( compType, t.tsym );
  }

  @Override
  public Type visitCapturedType( Type.CapturedType t, Void s )
  {
    Type w_bound = t.wildcard.type;
    w_bound = eraseBound( t, w_bound );
    if( w_bound == t.wildcard.type )
    {
      return t;
    }
    return new Type.CapturedType( t.tsym.name, t.tsym.owner, t.getUpperBound(), t.lower, t.wildcard );
  }

  @Override
  public Type visitTypeVar( Type.TypeVar t, Void s )
  {
    Type bound = eraseBound( t, t.getUpperBound() );
    Type lower = eraseBound( t, t.lower );
    if( bound == t.getUpperBound() && lower == t.lower )
    {
      return t;
    }
    return new Type.TypeVar( t.tsym, bound, lower );
  }

  @Override
  public Type visitWildcardType( Type.WildcardType t, Void s )
  {
    Type bound = eraseBound( t, t.type );
    if( bound == t.type )
    {
      return t;
    }
    return new Type.WildcardType( bound, t.kind, t.tsym );
  }

  @Override
  public Type visitType( Type t, Void o )
  {
    if( TypeUtil.isStructuralInterface( _extensionTransformer.getTypeProcessor(), t.tsym ) )
    {
      return _extensionTransformer.getObjectClass().asType();
    }
    return t;
  }

  private Type eraseBound( Type t, Type bound )
  {
    if( bound == null || bound instanceof NoType )
    {
      return bound;
    }

    Type erasedBound;
    if( bound.contains( t ) )
    {
      erasedBound = visit( _types.erasure( bound ) );
    }
    else
    {
      erasedBound = visit( bound );
    }
    return erasedBound;
  }
}