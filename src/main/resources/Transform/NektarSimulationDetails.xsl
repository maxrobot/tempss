<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exslt="http://exslt.org/common"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema">

  <!-- 
    -  Templates for generating Nektar++ boundary conditions and boundary 
    -  regions. This file is imported by other Nektar++ transforms.
   -->
<!--   <xsl:template match="*" mode="SimulationParameters">
    <xsl:message>Processing boundary conditions and regions.</xsl:message>
    
    <xsl:apply-templates select="Driver" mode="AddDriver" />
      
  </xsl:template> -->
  
  <xsl:template match="Driver" mode="AddDriver">
    <I PROPERTY="DRIVER">
      <xsl:attribute name="VALUE">
        <xsl:choose>
          <xsl:when test="Standard">Standard</xsl:when>
          <xsl:when test="Adaptive">Adaptive</xsl:when>
          <xsl:when test="ModifiedArnoldi">ModifiedArnoldi</xsl:when>
          <xsl:when test="SteadyState">SteadyState</xsl:when>
          <xsl:when test="Arpack">Arpack</xsl:when>
        </xsl:choose>
      </xsl:attribute>
    </I>    
  </xsl:template>

  <xsl:template match="Driver" mode="AddArpackType">
    <xsl:if test="Arpack">
      <I PROPERTY="ArpackProblemType">
        <xsl:attribute name="VALUE">
          <xsl:choose>
              <xsl:when test="Arpack = 'LargestMag'">LargestMag</xsl:when>
              <xsl:when test="Arpack = 'SmallestMag'">SmallestMag</xsl:when>
              <xsl:when test="Arpack = 'LargestReal'">LargestReal</xsl:when>
              <xsl:when test="Arpack = 'SmallestReal'">SmallestReal</xsl:when>
              <xsl:when test="Arpack = 'LargestImag'">LargestImag</xsl:when>
              <xsl:when test="Arpack = 'SmallestImag'">SmallestImag</xsl:when>
          </xsl:choose>
        </xsl:attribute>
      </I>        
    </xsl:if>
  </xsl:template>
                
  <xsl:template match="EvolutionOperator" mode="AddEvolution">
      <I PROPERTY="EvolutionOperator">
        <xsl:attribute name="VALUE">
          <xsl:choose>
            <xsl:when test="Direct">Direct</xsl:when>
            <xsl:when test="Adjoint">Adjoint</xsl:when>
            <xsl:when test="NonLinear">NonLinear</xsl:when>
            <xsl:when test="SkewSymmetric">SkewSymmetric</xsl:when>
            <xsl:when test="TransientGrowth">TransientGrowth</xsl:when>
            <xsl:when test="AdaptiveSFD">AdaptiveSFD</xsl:when>
          </xsl:choose>
        </xsl:attribute>
      </I>    
  </xsl:template>

  <xsl:template match="AdaptiveSFD" mode="AddSFDParams">
    <xsl:if test="kdim">
      <P> kdim = <xsl:value-of select="kdim" /> </P>
    </xsl:if>
    <xsl:if test="evtol">
      <P> evtol = <xsl:value-of select="evtol" /> </P>
    </xsl:if>
    <xsl:if test="nvec">
      <P> nvec = <xsl:value-of select="nvec" /> </P>
    </xsl:if>
    <xsl:if test="nits">
      <P> nits = <xsl:value-of select="nits" /> </P>
    </xsl:if>
  </xsl:template>

  <xsl:template match="ClassicalSFD" mode="AddSFDParams">
    <xsl:if test="ControlCoefficient">
      <P> ControlCoeff = <xsl:value-of select="ControlCoefficient" /> </P>
    </xsl:if>
    <xsl:if test="FilterWidth">
      <P> FilterWidth = <xsl:value-of select="FilterWidth" /> </P>
    </xsl:if>
    <xsl:if test="Tolerance">
      <P> TOL = <xsl:value-of select="Tolerance" /> </P>
    </xsl:if>
  </xsl:template>

  <xsl:template match="Advection" mode="AddAdvection">
      <I PROPERTY="AdvectionForm">
        <xsl:attribute name="VALUE">
          <xsl:choose>
            <xsl:when test="Convective">Convective</xsl:when>
            <xsl:when test="NonConservative">NonConservative</xsl:when>
            <xsl:when test="Linearised">Linearised</xsl:when>
            <xsl:when test="Adjoint">Adjoint</xsl:when>
            <xsl:when test="SkewSymmetric">SkewSymmetric</xsl:when>
            <xsl:when test="NoAdvection">NoAdvection</xsl:when>
          </xsl:choose>
        </xsl:attribute>
      </I>    
  </xsl:template>

  <xsl:template match="TimeIntegrationMethod" mode="AddTiming">
      <I PROPERTY="TimeIntegrationMethod">
        <xsl:attribute name="VALUE">
          <xsl:choose>
            <xsl:when test="IMEXOrder1">IMEXOrder1</xsl:when>
            <xsl:when test="IMEXOrder2">IMEXOrder2</xsl:when>
            <xsl:when test="IMEXOrder3">IMEXOrder3</xsl:when>
            <xsl:when test="DIRKOrder1">DIRKOrder1</xsl:when>
            <xsl:when test="ForwardEuler">ForwardEuler</xsl:when>
            <xsl:when test="BackwardEuler">BackwardEuler</xsl:when>
            <xsl:when test="ClassicalRungeKutta4">ClassicalRungeKutta4</xsl:when>
            <xsl:when test="BDFImplicitOrder1">BDFImplicitOrder1</xsl:when>
            <xsl:when test="BDFImplicitOrder2">BDFImplicitOrder2</xsl:when>
          </xsl:choose>
        </xsl:attribute>
      </I>    
  </xsl:template>

</xsl:stylesheet>