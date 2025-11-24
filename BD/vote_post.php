<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) {
    echo json_encode($arr);
    exit();
}

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) {
    // fallback to POST form
    $data = $_POST;
}

$postId = isset($data['postId']) ? (int)$data['postId'] : 0;
$userId = isset($data['userId']) ? (int)$data['userId'] : 0;
$vote   = isset($data['vote']) ? (int)$data['vote'] : 0; // expected 1 or -1

if ($postId <= 0 || $userId <= 0) {
    respond(['success' => false, 'message' => 'postId y userId requeridos']);
}
if (!in_array($vote, [1,-1], true)) {
    respond(['success' => false, 'message' => 'vote inválido (1 o -1)']);
}

$conn = DBConnection::getInstance()->getConnection();

// Implementación sin procedimientos almacenados
$conn->begin_transaction();

// Obtener voto previo
$prevVote = null;
$sel = $conn->prepare('SELECT vote FROM post_votes WHERE post_id = ? AND user_id = ?');
if ($sel) {
    $sel->bind_param('ii', $postId, $userId);
    $sel->execute();
    $res = $sel->get_result();
    if ($res && $res->num_rows) {
        $row = $res->fetch_assoc();
        $prevVote = (int)$row['vote'];
    }
    $sel->close();
}

$mensaje = '';
if ($prevVote === null) {
    // Insertar nuevo voto
    $ins = $conn->prepare('INSERT INTO post_votes (post_id, user_id, vote) VALUES (?,?,?)');
    if (!$ins) {
        $conn->rollback();
        respond(['success' => false, 'message' => 'Error preparando INSERT: '.$conn->error]);
    }
    $ins->bind_param('iii', $postId, $userId, $vote);
    $ok = $ins->execute();
    $ins->close();
    if (!$ok) { $conn->rollback(); respond(['success'=>false,'message'=>'Error insertando voto: '.$conn->error]); }
    $mensaje = 'Voto registrado.';
} elseif ($prevVote === $vote) {
    // Quitar voto (toggle)
    $del = $conn->prepare('DELETE FROM post_votes WHERE post_id = ? AND user_id = ?');
    if (!$del) { $conn->rollback(); respond(['success'=>false,'message'=>'Error preparando DELETE: '.$conn->error]); }
    $del->bind_param('ii', $postId, $userId);
    $ok = $del->execute();
    $del->close();
    if (!$ok) { $conn->rollback(); respond(['success'=>false,'message'=>'Error eliminando voto: '.$conn->error]); }
    $mensaje = 'Voto removido.';
} else {
    // Actualizar voto
    $upd = $conn->prepare('UPDATE post_votes SET vote = ? WHERE post_id = ? AND user_id = ?');
    if (!$upd) { $conn->rollback(); respond(['success'=>false,'message'=>'Error preparando UPDATE: '.$conn->error]); }
    $upd->bind_param('iii', $vote, $postId, $userId);
    $ok = $upd->execute();
    $upd->close();
    if (!$ok) { $conn->rollback(); respond(['success'=>false,'message'=>'Error actualizando voto: '.$conn->error]); }
    $mensaje = 'Voto actualizado.';
}

// Recalcular agregados
$likes = 0; $dislikes = 0;
$cnt1 = $conn->prepare('SELECT COUNT(*) AS c FROM post_votes WHERE post_id = ? AND vote = 1');
if ($cnt1) { $cnt1->bind_param('i', $postId); $cnt1->execute(); $r=$cnt1->get_result(); $likes = ($r&&$r->num_rows)? (int)$r->fetch_assoc()['c'] : 0; $cnt1->close(); }
$cnt2 = $conn->prepare('SELECT COUNT(*) AS c FROM post_votes WHERE post_id = ? AND vote = -1');
if ($cnt2) { $cnt2->bind_param('i', $postId); $cnt2->execute(); $r=$cnt2->get_result(); $dislikes = ($r&&$r->num_rows)? (int)$r->fetch_assoc()['c'] : 0; $cnt2->close(); }

$updAgg = $conn->prepare('UPDATE posts SET likes_count = ?, dislikes_count = ? WHERE post_id = ?');
if ($updAgg) {
    $updAgg->bind_param('iii', $likes, $dislikes, $postId);
    if (!$updAgg->execute()) { $conn->rollback(); respond(['success'=>false,'message'=>'Error actualizando totales: '.$conn->error]); }
    $updAgg->close();
}

$conn->commit();

// Devolver datos actualizados
$postStmt = $conn->prepare('SELECT p.post_id, p.likes_count, p.dislikes_count, 
    (SELECT vote FROM post_votes WHERE post_id = p.post_id AND user_id = ?) AS user_vote
  FROM posts p WHERE p.post_id = ? LIMIT 1');
if ($postStmt) {
    $postStmt->bind_param('ii', $userId, $postId);
    $postStmt->execute();
    $postRes = $postStmt->get_result();
    $postData = $postRes && $postRes->num_rows ? $postRes->fetch_assoc() : null;
    $postStmt->close();
} else {
    $postData = null;
}

respond([
    'success' => true,
    'message' => $mensaje,
    'post' => $postData
]);
?>
