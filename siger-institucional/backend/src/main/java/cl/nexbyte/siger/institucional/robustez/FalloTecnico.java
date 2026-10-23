package cl.nexbyte.siger.institucional.robustez;
public final class FalloTecnico extends RuntimeException{
    public final int status;
    public final String code;
    public FalloTecnico(int status){this(status,null);}
    public FalloTecnico(int status,String code){super("Operación técnica no completada");this.status=status;this.code=code;}
}
