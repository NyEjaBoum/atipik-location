package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.*;

import bean.CGenUtil;
import reservation.Reservation;
import reservation.ReservationLib;
import reservation.ReservationDetailsLib;
import caution.CautionLib;
import utilitaire.UtilDB;
import vente.Vente;

@Path("/reservation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationWS {

    @GET
    @Path("/fiche/{id}")
    public Response ficheReservation(@PathParam("id") String id) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            // Récupérer la réservation avec la même table que dans la JSP
            ReservationLib resa = new ReservationLib();
            // resa.setNomTable("RESERVATION_LIB_MIN_DATYF");
            resa.setNomTable("RESERVATION_LIB");
                        resa.setId(id);

resa = (ReservationLib) resa.getById(id, "RESERVATION_LIB", c);
            // resa = (ReservationLib) resa.getById(id, "RESERVATION_LIB_MIN_DATYF", c);
            
            if (resa == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Réservation introuvable");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            // Récupérer la caution (comme dans la JSP : CautionLib caution = t.getCautions())
            CautionLib caution = resa.getCautions();

            // Construire la fiche avec TOUS les champs comme dans la JSP
            Map<String, Object> fiche = new HashMap<>();
            fiche.put("id", resa.getId());
            fiche.put("idclient", resa.getIdclient());
            fiche.put("client", resa.getIdclientlib());
            fiche.put("idorigine", resa.getIdorigine());
            fiche.put("modelivraison", resa.getModelivraisonAffiche());
            fiche.put("date", resa.getDaty() != null ? resa.getDaty().toString() : null);
            fiche.put("remarque", resa.getRemarque());
            fiche.put("etat", resa.getEtat());
            fiche.put("etatLib", resa.getEtatlib());
            fiche.put("montant", resa.getMontant());
            fiche.put("montantTotal", resa.getMontanttotal());
            fiche.put("montantTva", resa.getMontantTva());
            fiche.put("montantTTC", resa.getMontantTTC());
            fiche.put("montantCaution", resa.getMontantcaution());
            fiche.put("montantRemise", resa.getMontantremise());
            fiche.put("paye", resa.getPaye());
            fiche.put("resteAPayer", resa.getResteAPayer());
            fiche.put("lieulocation", resa.getLieulocation());
            fiche.put("periode", resa.getPeriode());
            fiche.put("numBl", resa.getNumBl());
            fiche.put("equiperesp", resa.getEquiperesp());
            fiche.put("desceequiperesp", resa.getDesceequiperesp());
            fiche.put("revient", resa.getRevient());
            fiche.put("marge", resa.getMarge());
            fiche.put("datePrevisionRetour", resa.getDatePrevisionRetour() != null ? resa.getDatePrevisionRetour().toString() : null);
            fiche.put("datePrevisionDepart", resa.getDatePrevisionDepart() != null ? resa.getDatePrevisionDepart().toString() : null);

            // Informations sur la caution (si elle existe)
            if (caution != null) {
                fiche.put("cautionId", caution.getId());
                fiche.put("datyCaution", caution.getDaty() != null ? caution.getDaty().toString() : null);
                fiche.put("debitCaution", caution.getDebit());
                fiche.put("showReglerCaution", caution.getDaty() == null);
            } else {
                fiche.put("cautionId", null);
                fiche.put("datyCaution", null);
                fiche.put("debitCaution", 0);
                fiche.put("showReglerCaution", false);
            }

            // Récupérer les détails de la réservation
            ReservationDetailsLib detailCrit = new ReservationDetailsLib();
            detailCrit.setNomTable("RESERVATIONDETAILS_LIB");
            detailCrit.setIdmere(id);
            ReservationDetailsLib[] details = (ReservationDetailsLib[]) CGenUtil.rechercher(detailCrit, null, null, c, "");

            List<Map<String, Object>> detailsList = new ArrayList<>();
            if (details != null) {
                for (ReservationDetailsLib d : details) {
                    Map<String, Object> det = new HashMap<>();
                    det.put("id", d.getId());
                    det.put("produit", d.getLibelleproduit());
                    det.put("designation", d.getRemarque());
                    det.put("reference", d.getReferenceproduit());
                    det.put("image", d.getImage());
                    det.put("quantiteArticle", d.getQtearticle());
                    det.put("quantiteJour", d.getQte());
                    det.put("prixUnitaire", d.getPu());
                    det.put("montantRemise", d.getMontantremise());
                    det.put("montant", d.getMontant());
                    det.put("dateDebut", d.getDaty() != null ? d.getDaty().toString() : null);
                    detailsList.add(det);
                }
            }
            fiche.put("details", detailsList);

            // Récupérer toutes les cautions (pour l'onglet Caution)
            CautionLib cautionCrit = new CautionLib();
            cautionCrit.setNomTable("CAUTIONLIB");
            cautionCrit.setIdreservation(id);
            CautionLib[] cautions = (CautionLib[]) CGenUtil.rechercher(cautionCrit, null, null, c, "");

            List<Map<String, Object>> cautionsList = new ArrayList<>();
            if (cautions != null && cautions.length > 0) {
                for (CautionLib cau : cautions) {
                    Map<String, Object> cauMap = new HashMap<>();
                    cauMap.put("id", cau.getId());
                    cauMap.put("montant", cau.getMontantgrp());
                    cauMap.put("pctApplique", cau.getPct_applique());
                    //cauMap.put("etat", cau.getEtat());
                    cauMap.put("daty", cau.getDaty() != null ? cau.getDaty().toString() : null);
                    cauMap.put("debit", cau.getDebit());
                    cauMap.put("credit", cau.getCredit());
                    cautionsList.add(cauMap);
                }
            }
            fiche.put("cautions", cautionsList);

            // Récupérer l'ID de la vente liée (comme dans la JSP)
            try {
                Reservation reservationBase = new Reservation();
                reservationBase.setId(id);
                Vente[] ventes = (Vente[]) reservationBase.getFactureClient("VENTE_CPL", c);
                if (ventes != null && ventes.length > 0) {
                    fiche.put("idVente", ventes[0].getId());
                } else {
                    fiche.put("idVente", null);
                }
            } catch (Exception e) {
                fiche.put("idVente", null);
            }

            return Response.ok(fiche).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }

    @GET
    @Path("/liste")
    public Response listeReservations(
            @QueryParam("dateMin") String dateMin,
            @QueryParam("dateMax") String dateMax,
            @QueryParam("idClient") String idClient,
            @QueryParam("etatPayment") Integer etatPayment,
            @QueryParam("etatLogistique") Integer etatLogistique,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("npp") @DefaultValue("10") int npp
    ) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            ReservationLib crit = new ReservationLib();
            crit.setNomTable("RESERVATION_ETATLIB_F");

            StringBuilder where = new StringBuilder();
            if (dateMin != null && !dateMin.isEmpty()) {
                where.append(" AND daty >= TO_DATE('").append(dateMin).append("', 'YYYY-MM-DD')");
            }
            if (dateMax != null && !dateMax.isEmpty()) {
                where.append(" AND daty <= TO_DATE('").append(dateMax).append("', 'YYYY-MM-DD')");
            }
            if (idClient != null && !idClient.isEmpty()) {
                where.append(" AND idclient = '").append(idClient).append("'");
            }
            if (etatPayment != null) {
                where.append(" AND etatpayment = ").append(etatPayment);
            }
            if (etatLogistique != null) {
                where.append(" AND etatlogistique = ").append(etatLogistique);
            }

            ReservationLib[] reservations = (ReservationLib[]) CGenUtil.rechercher(
                crit, null, null, c, where.toString()
            );

            List<Map<String, Object>> result = new ArrayList<>();
            double sommeMontant = 0;

            if (reservations != null) {
                for (ReservationLib r : reservations) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("client", r.getIdclientlib());
                    map.put("date", r.getDaty() != null ? r.getDaty().toString() : null);
                    map.put("remarque", r.getRemarque());
                    map.put("etatPayment", r.getEtatpaymentlib());
                    map.put("etatLogistique", r.getEtatlogistiquelib());
                    map.put("montant", r.getMontant());
                    result.add(map);
                    sommeMontant += r.getMontant();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("total", result.size());
            response.put("sommeMontant", sommeMontant);

            return Response.ok(response).build();

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }
}