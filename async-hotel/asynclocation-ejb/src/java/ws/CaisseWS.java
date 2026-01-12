package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import caisse.MvtCaisseCaution;
import caution.Caution;
import caution.CautionLib;
import reservation.ReservationLib;
import utilitaire.UtilDB;
import utils.ConstanteLocation;

import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Path("/caisse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CaisseWS {


@GET
@Path("/mvt-caisse-liste")
@Produces(MediaType.APPLICATION_JSON)
public Response getListeMvtCaisse(
    @QueryParam("etat") String etat
) {
    Connection c = null;
    try {
        c = new UtilDB().GetConn();
        caisse.MvtCaisseCpl t = new caisse.MvtCaisseCpl();
        if (etat == null) etat = "";
        t.setNomTable(t.getNomTable().concat(etat));
        String[] listeCrt = {"id", "designation", "daty"};
        String[] listeInt = {"daty"};
        String[] libEntete = {"id", "daty", "designation", "idCaisseLib", "idVenteDetail", "idVirement", "credit", "debit", "etatLib"};
        Object[] result = bean.CGenUtil.rechercher(t, null, null, c, "");
        java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        for (Object obj : result) {
            caisse.MvtCaisseCpl mvt = (caisse.MvtCaisseCpl) obj;
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", mvt.getId());
            map.put("daty", mvt.getDaty());
            map.put("designation", mvt.getDesignation());
            map.put("idCaisseLib", mvt.getIdCaisseLib());
            map.put("idVenteDetail", mvt.getIdVente());
            map.put("idVirement", mvt.getIdVirement());
            map.put("credit", mvt.getCredit());
            map.put("debit", mvt.getDebit());
            map.put("etatLib", mvt.getEtatLib());
            data.add(map);
        }
        return Response.ok(data).build();
    } catch (Exception e) {
        e.printStackTrace();
        return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
    } finally {
        if (c != null) try { c.close(); } catch (Exception ignore) {}
    }
}


@POST
@Path("/mvt-location")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response insererMvtCaisseLocation(
    Map<String, Object> data,
    @Context HttpServletRequest request
) {
    Connection c = null;
    boolean estOuvert = false;
    try {
        HttpSession session = request.getSession();
        user.UserEJB u = (user.UserEJB) session.getAttribute("u");
        if (u == null || u.getUser() == null) {
            return Response.status(401).entity("{\"error\":\"Utilisateur non authentifié\"}").build();
        }
        String userId = u.getUser().getTuppleID();

        c = new UtilDB().GetConn();
        estOuvert = true;

        String datyStr = (data.get("daty") != null) ? data.get("daty").toString() : null;
        String designation = (data.get("designation") != null) ? data.get("designation").toString() : null;
        String idCaisse = (data.get("idCaisse") != null) ? data.get("idCaisse").toString() : null;
        String idOrigine = (data.get("idOrigine") != null) ? data.get("idOrigine").toString() : null; // id de la vente
        Double montant = (data.get("montant") != null) ? Double.parseDouble(data.get("montant").toString()) : null;
        String tiers = (data.get("tiers") != null) ? data.get("tiers").toString() : null;

        if (datyStr == null || designation == null || idCaisse == null || idOrigine == null || montant == null) {
            return Response.status(400).entity("{\"error\":\"Champs obligatoires manquants\"}").build();
        }

        caisse.MvtCaisse mvt = new caisse.MvtCaisse();
        mvt.setNomTable("MOUVEMENTCAISSE");
        mvt.setDaty(java.sql.Date.valueOf(datyStr));
        mvt.setDesignation(designation);
        mvt.setIdCaisse(idCaisse);
        mvt.setCredit(montant);
        mvt.setDebit(0);
        mvt.setIdOrigine(idOrigine);
        mvt.setIdTiers(tiers);

        String id = mvt.createObject(userId, c).getTuppleID();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("id", id);
        return Response.ok(resp).build();

    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", e.getMessage());
        return Response.serverError().entity(error).build();
    } finally {
        if (estOuvert && c != null) try { c.close(); } catch (Exception ignore) {}
    }
}

    @POST
    @Path("/mvt-caution")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insererMvtCaisseCaution(
        Map<String, Object> data,
        @Context HttpServletRequest request
    ) {
        Connection c = null;
        boolean estOuvert = false;
        try {
            HttpSession session = request.getSession();
            user.UserEJB u = (user.UserEJB) session.getAttribute("u");
            if (u == null || u.getUser() == null) {
                return Response.status(401).entity("{\"error\":\"Utilisateur non authentifié\"}").build();
            }
            String userId = u.getUser().getTuppleID();

            c = new UtilDB().GetConn();
            estOuvert = true;

            // Champs obligatoires
            String datyStr = (data.get("daty") != null) ? data.get("daty").toString() : null;
            String designation = (data.get("designation") != null) ? data.get("designation").toString() : null;
            String idCaisse = (data.get("idCaisse") != null) ? data.get("idCaisse").toString() : null;
            String type = (data.get("type") != null) ? data.get("type").toString() : null;
            String idcaution = (data.get("idcaution") != null) ? data.get("idcaution").toString() : null;

            if (datyStr == null || designation == null || idCaisse == null || type == null || idcaution == null) {
                return Response.status(400).entity("{\"error\":\"Champs obligatoires manquants\"}").build();
            }

            // Récupérer la caution (même logique que apresMvtCaution.jsp)
            CautionLib cl = (CautionLib) new CautionLib().getById(idcaution, "CautionLib", c);
            if (cl == null) {
                return Response.status(400).entity("{\"error\":\"Caution introuvable\"}").build();
            }

            String id = "";

            if ("regler".equalsIgnoreCase(type)) {
                // Remboursement (même logique que apresMvtCaution.jsp)
                MvtCaisseCaution remb = new MvtCaisseCaution();
                remb.setNomTable("MOUVEMENTCAISSE");
                remb.setDaty(java.sql.Date.valueOf(datyStr));
                remb.setDesignation("Paiement remboursement du caution " + idcaution);
                remb.setIdCaisse(idCaisse);
                remb.setType_mvt(ConstanteLocation.type_remboursement);
                remb.setDebit(cl.getMontantgrp());
                remb.setCredit(0);
                remb.setIdOrigine(cl.getId());
                id = remb.createObject(userId, c).getTuppleID();

                // Retenue
                ReservationLib res = cl.getReservationAvecVerif(c);
                double montantRetenue = res.getMontantRetenue(cl);
                double credit = montantRetenue;

                if (credit > 0) {
                    MvtCaisseCaution retenue = new MvtCaisseCaution();
                    retenue.setNomTable("MOUVEMENTCAISSE");
                    retenue.setDaty(java.sql.Date.valueOf(datyStr));
                    retenue.setDesignation("Paiement retenue du caution " + idcaution);
                    retenue.setIdCaisse(idCaisse);
                    retenue.setType_mvt(ConstanteLocation.type_retenue);
                    retenue.setDebit(0);
                    retenue.setCredit(credit);
                    retenue.setIdOrigine(cl.getId());
                    id = retenue.createObject(userId, c).getTuppleID();
                }

            } else if ("encaisser".equalsIgnoreCase(type)) {
                // Encaissement (même logique que apresMvtCaution.jsp)
                MvtCaisseCaution encaisser = new MvtCaisseCaution();
                encaisser.setNomTable("MOUVEMENTCAISSE");
                encaisser.setDaty(java.sql.Date.valueOf(datyStr));
                encaisser.setDesignation("Paiement encaisser du caution " + idcaution);
                encaisser.setIdCaisse(idCaisse);
                encaisser.setType_mvt(ConstanteLocation.type_encaissement);
                encaisser.setDebit(0);
                encaisser.setCredit(cl.getMontantgrp());
                encaisser.setIdOrigine(cl.getId());
                id = encaisser.createObject(userId, c).getTuppleID();
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("id", id);
            return Response.ok(resp).build();

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (estOuvert && c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }

    @GET
    @Path("/caution-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCautionStats(
        @QueryParam("idcaution") String idcaution
    ) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            CautionLib caution = (CautionLib) new CautionLib().getById(idcaution, "CautionLib", c);

            // Montant total de la caution
            double montantTotal = caution.getMontantgrp();

            // Montant déjà payé (somme des mouvements de caisse "encaisser" pour cette caution)
            String sql = "SELECT NVL(SUM(credit),0) FROM MOUVEMENTCAISSE WHERE idOrigine=? AND type_mvt=?";
            java.sql.PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, idcaution);
            ps.setString(2, ConstanteLocation.type_encaissement);
            java.sql.ResultSet rs = ps.executeQuery();
            double montantPaye = 0;
            if (rs.next()) montantPaye = rs.getDouble(1);
            rs.close();
            ps.close();

            double montantReste = montantTotal - montantPaye;

            Map<String, Object> resp = new HashMap<>();
            resp.put("montantTotal", montantTotal);
            resp.put("montantPaye", montantPaye);
            resp.put("montantReste", montantReste);
            return Response.ok(resp).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }

    @GET
    @Path("/liste")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListeCaisses() {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
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

            // Récupérer la caution (même logique que mvtcaisse-saisie-caution.jsp)
            CautionLib cau = (CautionLib) new CautionLib().getById(idcaution, "CautionLib", c);

            // Récupérer la réservation liée
            ReservationLib res = cau.getReservationAvecVerif(c);

            double montantRetenue = res.getMontantRetenue(cau);
            double credit = montantRetenue;
            double debit = cau.getMontantgrp() - montantRetenue;

            // Montant à payer pour encaissement (même logique que mvtcaisse-saisie-caution.jsp)
            double payer = 0;
            if ("encaisser".equalsIgnoreCase(type)) {
                Caution caution = (Caution) new Caution().getById(idcaution, "CAUTION", c);
                // payer = (caution.getMontantreservation() * caution.getPct_applique()) / 100;
                payer = cau.getMontantgrp(); // Montant de la caution déjà calculé et stocké
            }

            Map<String, Object> form = new HashMap<>();
            form.put("daty", utilitaire.Utilitaire.dateDuJour());
            form.put("designation", "Paiement du " + utilitaire.Utilitaire.dateDuJour());
            form.put("tiers", res.getIdclient());
            form.put("idCaisse", null);

            if ("encaisser".equalsIgnoreCase(type)) {
                form.put("debit", 0);
                form.put("credit", payer);
            } else {
                form.put("debit", debit);
                form.put("credit", credit);
            }

            form.put("type", type);
            form.put("idcaution", idcaution);
            form.put("idreservation", idreservation);
            form.put("showDebit", !"encaisser".equalsIgnoreCase(type));
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