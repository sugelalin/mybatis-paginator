package com.github.miemiedev.mybatis.paginator.dialect;

import com.github.miemiedev.mybatis.paginator.domain.Order;
import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 类似hibernate的Dialect,但只精简出分页部分
 * @author badqiu
 * @author miemiedev
 */
public class Dialect {
    protected TypeHandlerRegistry typeHandlerRegistry;
    protected MappedStatement mappedStatement;
    protected PageBounds pageBounds;
    protected Object parameterObject;
    protected BoundSql boundSql;
    protected List<ParameterMapping> parameterMappings;
    protected Map<String, Object> pageParameters = new HashMap<String, Object>();

    private String pageSQL;
    private String countSQL;
    private String simpleCountSQL;

    private static final Pattern ORDER_BY = Pattern.compile("order by .*(desc|asc)");


    public Dialect(MappedStatement mappedStatement, Object parameterObject, PageBounds pageBounds){
        this.mappedStatement = mappedStatement;
        this.parameterObject = parameterObject;
        this.pageBounds = pageBounds;
        this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();

        init();
    }

    protected void init(){

        boundSql = mappedStatement.getBoundSql(parameterObject);
        parameterMappings = new ArrayList(boundSql.getParameterMappings());
        if(parameterObject instanceof Map){
            pageParameters.putAll((Map)parameterObject);
        }else if( parameterObject != null){
            Class cls = parameterObject.getClass();
            if(cls.isPrimitive() || cls.isArray() ||
                    SimpleTypeRegistry.isSimpleType(cls) ||
                    Enum.class.isAssignableFrom(cls) ||
                    Collection.class.isAssignableFrom(cls)){
                for (ParameterMapping parameterMapping : parameterMappings) {
                    pageParameters.put(parameterMapping.getProperty(),parameterObject);
                }
            }else{
                MetaObject metaObject = mappedStatement.getConfiguration().newMetaObject(parameterObject);
                ObjectWrapper wrapper = metaObject.getObjectWrapper();
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String property = parameterMapping.getProperty();
                    if (wrapper.hasGetter(property)){
                        PropertyTokenizer prop = new PropertyTokenizer(property);
                        pageParameters.put(property,wrapper.get(prop));
                    } else {
                        pageParameters.put(property,
                                boundSql.getAdditionalParameter(property));
                    }

                }
            }

        }

        StringBuffer bufferSql = new StringBuffer(boundSql.getSql().trim());
        if(bufferSql.lastIndexOf(";") == bufferSql.length()-1){
            bufferSql.deleteCharAt(bufferSql.length()-1);
        }
        String sql = bufferSql.toString();
        pageSQL = sql;
        if(pageBounds.getOrders() != null && !pageBounds.getOrders().isEmpty()){
            pageSQL = getSortString(sql, pageBounds.getOrders());
        }
        if(pageBounds.getOffset() != RowBounds.NO_ROW_OFFSET
                || pageBounds.getLimit() != RowBounds.NO_ROW_LIMIT){
            pageSQL = getLimitString(pageSQL, "__offset", pageBounds.getOffset(), "__limit",pageBounds.getLimit());
        }


        countSQL = getCountString(sql);
        simpleCountSQL = getSimpleCountString(sql);
    }


    public List<ParameterMapping> getParameterMappings(){
        return parameterMappings;
    }

    public Object getParameterObject(){
        return pageParameters;
    }


    public String getPageSQL(){
        return pageSQL;
    }

    protected void setPageParameter(String name, Object value, Class type){
        ParameterMapping parameterMapping = new ParameterMapping.Builder(mappedStatement.getConfiguration(), name, type).build();
        parameterMappings.add(parameterMapping);
        pageParameters.put(name, value);
    }


    public String getCountSQL(){
        return countSQL;
    }

    public String getSimpleCountSQL() {
        return simpleCountSQL;
    }


    /**
     * 将sql变成分页sql语句
     */
    protected String getLimitString(String sql, String offsetName,int offset, String limitName, int limit) {
        throw new UnsupportedOperationException("paged queries not supported");
    }

    /**
     * 将sql转换为总记录数SQL
     * @param sql SQL语句
     * @return 总记录数的sql
     */
    protected String getCountString(String sql){
        return "select count(1) from (" + sql + ") tmp_count";
    }

    /**
     * 将sql转换为总记录数SQL，并尝试去掉 order by
     * @param sql
     * @return
     */
    protected String getSimpleCountString(String sql) {
        String[] orderArr = sql.split("order by");
        // step1 如果 sql中有多个 order by 则不转换
        if (orderArr.length > 2) {
            return getCountString(sql);
        }
        // step2 去掉 sql中的 order by xxx desc/asc 语句
        return  "select count(1) from (" + ORDER_BY.matcher(sql).replaceAll(" ") + ") tmp_count";
    }

    /**
     * 将sql转换为带排序的SQL
     * @param sql SQL语句
     * @return 总记录数的sql
     */
    protected String getSortString(String sql, List<Order> orders){
        if(orders == null || orders.isEmpty()){
            return sql;
        }

        StringBuffer buffer = new StringBuffer("select * from (").append(sql).append(") temp_order order by ");
        for(Order order : orders){
            if(order != null){
                buffer.append(order.toString())
                        .append(", ");
            }

        }
        buffer.delete(buffer.length()-2, buffer.length());
        return buffer.toString();
    }
}
