package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import caisse.Caisse;
import caisse.MvtCaisseCaution;
import caution.CautionLib;
import reservation.ReservationLib;
import utilitaire.UtilDB;
import javax.ws.rs.core.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Path("/caisse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CaisseWS {



@POST
@Path("/mvt-caution")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response insererMvtCaisseCaution(
    Map<String, Object> data,
    @Context HttpServletRequest request
) {
    Connection c = null;
    try {
        HttpSession session = request.getSession();
        user.UserEJB u = (user.UserEJB) session.getAttribute("u");
        if (u == null || u.getUser() == null) {
            return Response.status(401).entity("{\"error\":\"Utilisateur non authentifié\"}").build();
        }
        String userId = u.getUser().getTuppleID();

        c = new utilitaire.UtilDB().GetConn();
        c.setAutoCommit(false);

        caisse.MvtCaisseCaution mvt = new caisse.MvtCaisseCaution();

        // Champs obligatoires
        String datyStr = (data.get("daty") != null) ? data.get("daty").toString() : null;
        String designation = (data.get("designation") != null) ? data.get("designation").toString() : null;
        String idCaisse = (data.get("idCaisse") != null) ? data.get("idCaisse").toString() : null;
        String type = (data.get("type") != null) ? data.get("type").toString() : null;
        String idcaution = (data.get("idcaution") != null) ? data.get("idcaution").toString() : null;

        if (datyStr == null || designation == null || idCaisse == null || type == null || idcaution == null) {
            return Response.status(400).entity("{\"error\":\"Champs obligatoires manquants\"}").build();
        }

        mvt.setDaty(java.sql.Date.valueOf(datyStr));
        mvt.setDesignation(designation);
        mvt.setIdCaisse(idCaisse);
        mvt.setIdOrigine(idcaution);
        //mvt.setType_mvt(type);

        // Gestion du sens du mouvement
        double credit = data.get("credit") != null ? Double.parseDouble(data.get("credit").toString()) : 0;
        double debit = data.get("debit") != null ? Double.parseDouble(data.get("debit").toString()) : 0;

        if ("encaisser".equalsIgnoreCase(type)) { //ampina designatuin
            mvt.setCredit(credit);
            mvt.setDebit(0);
        } else if ("regler".equalsIgnoreCase(type)) { //ampina designatuin
            mvt.setCredit(0);
            mvt.setDebit(debit);
        } else {
            mvt.setCredit(credit);
            mvt.setDebit(debit);
        }

        // Log pour debug
        System.out.println("[DEBUG] MvtCaisseCaution à insérer :");
        System.out.println("  idCaisse=" + mvt.getIdCaisse());
        System.out.println("  idOrigine=" + mvt.getIdOrigine());
        System.out.println("  daty=" + mvt.getDaty());
        System.out.println("  designation=" + mvt.getDesignation());
        System.out.println("  credit=" + mvt.getCredit());
        System.out.println("  debit=" + mvt.getDebit());
        System.out.println("  type_mvt=" + mvt.getType_mvt());

        caisse.MvtCaisseCaution inserted = (caisse.MvtCaisseCaution) mvt.createObject(userId, c);

        c.commit();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("id", inserted.getId());
        return Response.ok(resp).build();
    } catch (Exception e) {
        e.printStackTrace();
        if (c != null) try { c.rollback(); } catch (Exception ignore) {}
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListeCaisses() {
        java.sql.Connection c = null;
        try {
            c = new utilitaire.UtilDB().GetConn();
            caisse.Caisse caisse = new caisse.Caisse();
            caisse.Caisse[] caisses = (caisse.Caisse[]) bean.CGenUtil.rechercher(caisse, null, null, c, "");
            java.util.List<java.util.Map<String, String>> result = new java.util.ArrayList<>();
            for (caisse.Caisse cs : caisses) {
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("id", cs.getId());
                map.put("val", cs.getVal());
                result.add(map);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }

    @GET
    @Path("/formulaire-mvt-caution")
    public Response getFormulaireMvtCaution(
            @QueryParam("idcaution") String idcaution,
            @QueryParam("idreservation") String idreservation,
            @QueryParam("type") String type
    ) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            // Récupérer la caution
            CautionLib cau = new CautionLib();
            cau.setId(idcaution);
            cau.setIdreservation(idreservation);
            cau = (CautionLib) new CautionLib().getById(idcaution, "CAUTIONLIB", c);

            // Récupérer la réservation liée
            ReservationLib res = cau.getReservationAvecVerif(c);

            double montantRetenue = res.getMontantRetenue(cau);
            double credit = montantRetenue;
            double debit = cau.getMontantgrp() - montantRetenue;

            Map<String, Object> form = new HashMap<>();
            form.put("daty", utilitaire.Utilitaire.dateDuJour());
            form.put("designation", "Paiement du " + utilitaire.Utilitaire.dateDuJour());
            form.put("tiers", res.getIdclient());
            form.put("idCaisse", null); // à remplir côté front avec la liste des caisses
            form.put("debit", type != null && type.equalsIgnoreCase("encaisser") ? 0 : debit);
            form.put("credit", type != null && type.equalsIgnoreCase("encaisser") ? debit : credit);
            form.put("type", type);
            form.put("idcaution", idcaution);
            form.put("idreservation", idreservation);
            form.put("showDebit", !(type != null && type.equalsIgnoreCase("encaisser")));
            form.put("showCredit", true);

            return Response.ok(form).build();
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
}