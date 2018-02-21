package br.com.lucas.cas.adaptors.x509.authentication.principal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.security.cert.CertificateParsingException;
import java.util.List;

import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.apereo.cas.adaptors.x509.authentication.principal.AbstractX509PrincipalResolver;
import org.ldaptive.LdapException;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Credential to principal resolver that extracts Subject Alternative Name ICP-Brasil CPF/CNPJ
 * extension from the provided certificate if available as a resolved principal id.
*/


/**
 * This is {@link X509SubjectICPBrasilPrincipalResolver}.
 *
 * @author Lucas Rogerio Caetano Ferreira
*/
public class X509SubjectICPBrasilPrincipalResolver extends AbstractX509PrincipalResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(X509SubjectICPBrasilPrincipalResolver.class);

	private DnResolver dnResolver;

	public X509SubjectICPBrasilPrincipalResolver(final IPersonAttributeDao attributeRepository, final PrincipalFactory principalFactory,
												 final boolean returnNullIfNoAttributes,
												 final String principalAttributeName) {
		super(attributeRepository, principalFactory, returnNullIfNoAttributes, principalAttributeName);
	}

	/**
	 * In the first 8 (eight) positions, the date of birth of the natural person holding the
     * certificate, in ddmmaaaa format; in the 11 (eleven) subsequent positions, the registration
     * number in the Individual Taxpayer's Registry (CPF) of the individual holder of the certificate; in the
     * 11 (eleven) subsequent positions, the Social Identification number of the individual holder of the
     * certificate - NIS (PIS, PASEP or CI); in the 15 (fifteen) subsequent positions, the
     * General Registry number - RG of the natural person holding the certificate; in the six (6)
     * subsequent positions, the acronym of the issuing body of the GR and its UF.
     *
     * @see <a href="http://www.iti.gov.br/images/twiki/URL/pub/Certificacao/DocIcp/docs13082012/ADE_ICP-04.01_-_v_4.1_Esquema_DE_OID.pdf">
     *     ICPBrasil OID Schema</a>
     * @see <a href="http://www.iti.gov.br/images/icp-brasil/legislacao/Resolucoes/RESOLU__O_41_DE_18_04_2006.PDF">
     *     ICPBrasil OID Resolution</a>
	 */
	protected static final String PESSOA_FISICA_OBJECTID = "2.16.76.1.3.1";

	/**
	 * Registration number in the National Register of Legal Entities (CNPJ) of the Legal Entity holder of the certificate.
	 *
	 * @see <a href="http://www.iti.gov.br/images/twiki/URL/pub/Certificacao/DocIcp/docs13082012/ADE_ICP-04.01_-_v_4.1_Esquema_DE_OID.pdf">
     *     ICPBrasil OID Schema</a>
     * @see <a href="http://www.iti.gov.br/images/icp-brasil/legislacao/Resolucoes/RESOLU__O_41_DE_18_04_2006.PDF">
     *     ICPBrasil OID Resolution</a>
	 */
	protected static final String PESSOA_JURIDICA_OBJECTID = "2.16.76.1.3.3";

	/**
     * Retrieves Subject Alternative Name ICP-Brasil CPF/CNPJ extension as a principal id String.
     *
     * @param certificate X.509 certificate credential.
     *
     * @return Resolved principal ID or null if no SAN ICP-Brasil CPF/CNPJ extension is available in provided certificate.
     *
     * @see AbstractX509PrincipalResolver#resolvePrincipalInternal(java.security.cert.X509Certificate)
     * @see java.security.cert.X509Certificate#getSubjectAlternativeNames()
     */
	@Override
	protected String resolvePrincipalInternal(final X509Certificate certificate) {
		try {
            final Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames != null) {
                for (final List<?> sanItem : subjectAltNames) {
                    final ASN1Sequence seq = getAltnameSequence(sanItem);
                    final String ICPBrasilString = getICPBrasilStringFromSequence(seq);
                    if (ICPBrasilString != null) {
                    	if (dnResolver != null) {
            				try {
            					final String dn = dnResolver.resolve(new User(ICPBrasilString));
            					if (dn != null && !dn.isEmpty()) {
            						LOGGER.debug("Found DN [" + dn + "] resolved through digital certificate extracted id [" + ICPBrasilString +"].");
            						return ICPBrasilString;
            					}
            					else {
									LOGGER.debug("DN not found through digital certificate extracted id [" + ICPBrasilString +"].");
            						return null;
            					}
            				} catch (LdapException e) {
								LOGGER.error("Erro ao resolver DN : {}",e);
            				}
                    	}
                        return ICPBrasilString;
                    }
                }
            }
        } catch (final CertificateParsingException e) {
			LOGGER.error("Error is encountered while trying to retrieve subject alternative names collection from certificate", e);
			LOGGER.debug("Returning null principal id...");
            return null;
        }
		LOGGER.debug("Returning null principal id...");
		return null;
	}

	/**
     * Get ICPBrasil CPF/CNPJ String.
     *
     * @param seq ASN1Sequence abstraction representing subject alternative name.
     * First element is the object identifier, second is the object itself.
     *
     * @return ICPBrasil CPF/CNPJ string or null
     */
    private String getICPBrasilStringFromSequence(final ASN1Sequence seq) {
        if (seq != null) {
            // First in sequence is the object identifier, that we must check
            final ASN1ObjectIdentifier id = ASN1ObjectIdentifier.getInstance(seq.getObjectAt(0));
            if (id != null) {
            	final boolean isPessoaFisica =  PESSOA_FISICA_OBJECTID.equals(id.getId());
            	final boolean isPessoaJuridica =  PESSOA_JURIDICA_OBJECTID.equals(id.getId());
				LOGGER.debug("isPessoaFisica: " + isPessoaFisica + " -- isPessoaJuridica: " + isPessoaJuridica);
            	if (isPessoaFisica || isPessoaJuridica) {
	                final ASN1TaggedObject obj = (ASN1TaggedObject) seq.getObjectAt(1);
	                ASN1Primitive prim = obj.getObject();

	                // Due to bug in java cert.getSubjectAltName, it can be tagged an extra time
	                if (prim instanceof ASN1TaggedObject) {
	                    prim = ASN1TaggedObject.getInstance(((ASN1TaggedObject) prim)).getObject();
	                }
	                String content = null;
	                if (prim instanceof ASN1OctetString) {
	                	content =  new String(((ASN1OctetString) prim).getOctets());
	                } else if (prim instanceof ASN1String) {
	                	content = ((ASN1String) prim).getString();
	                } else{
	                    return null;
	                }

	                if (isPessoaFisica && content.length() >= 20) {
						LOGGER.debug("Returning CPF through Pessoa Fisica ObjectID content [" + content + "]");
	                	return content.substring(8, 19);
	                }
	                else if (isPessoaJuridica) {
						LOGGER.debug("Returning CNPJ through Pessoa Juridica ObjectID content [" + content + "]");
	                	return content;
	                }
	                return null;
            	}
            }
        }
        return null;
    }

    /**
     * Get alt name seq.
     *
     * @param sanItem subject alternative name value encoded as a two elements List with elem(0) representing object id and elem(1)
     * representing object (subject alternative name) itself.
     *
     * @return ASN1Sequence abstraction representing subject alternative name or null if the passed in
     * List doesn't contain at least to elements
     * as expected to be returned by implementation of {@code X509Certificate.html#getSubjectAlternativeNames}
     *
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/security/cert/X509Certificate.html#getSubjectAlternativeNames()">
     *     X509Certificate#getSubjectAlternativeNames</a>
     */
    private ASN1Sequence getAltnameSequence(final List<?> sanItem) {
        //Should not be the case, but still, a extra "safety" check
        if (sanItem.size() < 2) {
			LOGGER.error("Subject Alternative Name List does not contain at least two required elements. Returning null principal id...");
        }
        final Integer itemType = (Integer) sanItem.get(0);
        if (itemType == 0) {
            final byte[] altName = (byte[]) sanItem.get(1);
            return getAltnameSequence(altName);
        }
        return null;
    }

    /**
     * Get alt name seq.
     *
     * @param sanValue subject alternative name value encoded as byte[]
     *
     * @return ASN1Sequence abstraction representing subject alternative name
     *
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/security/cert/X509Certificate.html#getSubjectAlternativeNames()">
     *     X509Certificate#getSubjectAlternativeNames</a>
     */
    private ASN1Sequence getAltnameSequence(final byte[] sanValue) {
        ASN1Primitive oct = null;
        try (final ByteArrayInputStream bInput = new ByteArrayInputStream(sanValue)) {
            try (final ASN1InputStream input = new ASN1InputStream(bInput)) {
                oct = input.readObject();
            } catch (final IOException e) {
				LOGGER.error("Error on getting Alt Name as a DERSEquence: {}", e.getMessage(), e);
            }
            return ASN1Sequence.getInstance(oct);
        } catch (final IOException e) {
			LOGGER.error("An error has occurred while reading the subject alternative name value", e);
        }
        return null;
    }

	public DnResolver getDnResolver() {
		return dnResolver;
	}

	public void setDnResolver(final DnResolver resolver) {
		dnResolver = resolver;
	}
}
