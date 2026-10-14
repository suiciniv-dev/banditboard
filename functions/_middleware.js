export const onRequest = ({ request, next }) => {
  const url = new URL(request.url);
  if (url.hostname !== "banditboard.pages.dev") return next();
  url.hostname = "banditboard.suiciniv.dev";
  return Response.redirect(url.toString(), 301);
};
